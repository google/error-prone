/*
 * Copyright 2016 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.collectionincompatibletype.AbstractCollectionIncompatibleTypeMatcher.extractTypeArgAsMemberOfSupertype;

import com.google.auto.value.AutoValue;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CompatibleWith;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.EqualsIncompatibleType;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeParameterElement;

/** @author glorioso@google.com (Nick Glorioso) */
@BugPattern(
  name = "IncompatibleArgumentType",
  summary = "Passing argument to a generic method with an incompatible type.",
  category = JDK,
  severity = ERROR
)
public class IncompatibleArgumentType extends BugChecker implements MethodInvocationTreeMatcher {

  // Nonnull requiredType: The type I need is bound, in requiredType
  // null requiredType: I found the type variable, but I can't bind it to any type
  @AutoValue
  abstract static class RequiredType {
    abstract @Nullable Type type();

    static RequiredType create(Type type) {
      return new AutoValue_IncompatibleArgumentType_RequiredType(type);
    }
  }

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    // example:
    // class Foo<A> {
    //   <B> void bar(@CompatibleWith("A") Object o, @CompatibleWith("B") Object o2) {}
    // }
    // new Foo<Integer>().<String>bar(1, "a');

    // A Type substitution capturing <Integer> on Foo and <String> on bar(Object,Object);
    Type calledMethodType = ASTHelpers.getType(methodInvocationTree.getMethodSelect());
    // A Type substitution capturing <Integer> on Foo
    Type calledClazzType = ASTHelpers.getReceiverType(methodInvocationTree);

    List<? extends ExpressionTree> arguments = methodInvocationTree.getArguments();
    // The unbound MethodSymbol for bar(), with type parameters <A> and <B>
    MethodSymbol declaredMethod = ASTHelpers.getSymbol(methodInvocationTree);
    if (arguments.isEmpty() || declaredMethod == null) {
      return null;
    }

    List<RequiredType> requiredTypesAtCallSite =
        new ArrayList<>(Collections.nCopies(arguments.size(), null));

    Types types = state.getTypes();

    if (!populateTypesToEnforce(
        declaredMethod, calledMethodType, calledClazzType, requiredTypesAtCallSite, state)) {
      // No annotations on this method, try the supers;
      for (MethodSymbol method : ASTHelpers.findSuperMethods(declaredMethod, types)) {
        if (populateTypesToEnforce(
            method, calledMethodType, calledClazzType, requiredTypesAtCallSite, state)) {
          break;
        }
      }
    }

    reportAnyViolations(arguments, requiredTypesAtCallSite, state);
    // We manually report ourselves, so we don't pass any errors up the chain.
    return Description.NO_MATCH;
  }

  private void reportAnyViolations(
      List<? extends ExpressionTree> arguments,
      List<RequiredType> requiredArgumentTypes,
      VisitorState state) {
    Types types = state.getTypes();
    for (int i = 0; i < requiredArgumentTypes.size(); i++) {
      RequiredType requiredType = requiredArgumentTypes.get(i);
      if (requiredType == null) {
        continue;
      }
      ExpressionTree argument = arguments.get(i);
      Type argType = ASTHelpers.getType(argument);
      if (requiredType.type() != null) {
        // Report a violation for this type
        EqualsIncompatibleType.TypeCompatibilityReport report =
            EqualsIncompatibleType.compatibilityOfTypes(requiredType.type(), argType, state);
        if (!report.compatible()) {
          state.reportMatch(describeViolation(argument, argType, requiredType.type(), types));
        }
      }
    }
  }

  private Description describeViolation(
      ExpressionTree argument, Type argType, Type requiredType, Types types) {
    // For the error message, use simple names instead of fully qualified names unless they are
    // identical.
    String sourceType = Signatures.prettyType(argType);
    String targetType = Signatures.prettyType(ASTHelpers.getUpperBound(requiredType, types));
    if (sourceType.equals(targetType)) {
      sourceType = argType.toString();
      targetType = requiredType.toString();
    }

    String msg =
        String.format(
            "Argument '%s' should not be passed to this method. Its type %s is not"
                + " compatible with the required type: %s.",
            argument, sourceType, targetType);

    return buildDescription(argument).setMessage(msg).build();
  }

  // Return whether this method contains any @CompatibleWith annotations. If there are none, the
  // caller should explore super-methods.
  @CheckReturnValue
  private boolean populateTypesToEnforce(
      MethodSymbol declaredMethod,
      Type calledMethodType,
      Type calledReceiverType,
      List<RequiredType> argumentTypeRequirements,
      VisitorState state) {
    boolean foundAnyTypeToEnforce = false;
    List<VarSymbol> params = declaredMethod.params();
    for (int i = 0; i < params.size(); i++) {
      VarSymbol varSymbol = params.get(i);
      CompatibleWith anno = ASTHelpers.getAnnotation(varSymbol, CompatibleWith.class);
      if (anno != null) {
        foundAnyTypeToEnforce = true;

        // Now we try and resolve the generic type argument in the annotation against the current
        // method call's projection of this generic type.
        RequiredType requiredType =
            resolveRequiredTypeForThisCall(
                state, calledMethodType, calledReceiverType, declaredMethod, anno.value());

        // @CW is on the varags parameter
        if (declaredMethod.isVarArgs() && i == params.size() - 1) {
          if (i >= argumentTypeRequirements.size()) {
            // varargs method with 0 args passed from the caller side, no arguments to enforce
            // void foo(String...); foo();
            break;
          } else {
            // Set this required type for all of the arguments in the varargs position.
            for (int j = i; j < argumentTypeRequirements.size(); j++) {
              argumentTypeRequirements.set(j, requiredType);
            }
          }
        } else {
          argumentTypeRequirements.set(i, requiredType);
        }
      }
    }

    return foundAnyTypeToEnforce;
  }

  @Nullable
  @CheckReturnValue
  // From calledReceiverType
  private RequiredType resolveRequiredTypeForThisCall(
      VisitorState state,
      Type calledMethodType,
      Type calledReceiverType,
      MethodSymbol declaredMethod,
      String typeArgName) {
    RequiredType requiredType =
        resolveTypeFromGenericMethod(calledMethodType, declaredMethod, typeArgName);

    if (requiredType == null) {
      requiredType =
          resolveTypeFromClass(
              calledReceiverType, (ClassSymbol) declaredMethod.owner, typeArgName, state);
    }
    return requiredType;
  }

  private RequiredType resolveTypeFromGenericMethod(
      Type calledMethodType, MethodSymbol declaredMethod, String typeArgName) {
    int tyargIndex = findTypeArgInList(declaredMethod, typeArgName);
    return tyargIndex == -1
        ? null
        : RequiredType.create(getTypeFromTypeMapping(calledMethodType, typeArgName));
  }

  @SuppressWarnings("unchecked")
  // Plumb through a type which is supposed to be a Types.Subst, then find the replacement
  // type that the compiler resolved.
  private static Type getTypeFromTypeMapping(Type m, String namedTypeArg) {
    try {
      // Reflectively extract the mapping from an enclosing instance of Types.Subst
      Field substField = m.getClass().getDeclaredField("this$0");
      substField.setAccessible(true);
      Object subst = substField.get(m);
      Field fromField = subst.getClass().getDeclaredField("from");
      Field toField = subst.getClass().getDeclaredField("to");
      fromField.setAccessible(true);
      toField.setAccessible(true);

      // Search for `namedTypeArg` in `from`, and return the parallel instance in `to`.
      List<Type> types = (List<Type>) fromField.get(subst);
      List<Type> calledTypes = (List<Type>) toField.get(subst);
      for (int i = 0; i < types.size(); i++) {
        Type type = types.get(i);
        if (type.toString().equals(namedTypeArg)) {
          return calledTypes.get(i);
        }
      }
    } catch (ReflectiveOperationException ignored) {
      // Nothing we can do here.
    }
    return null;
  }

  // class Foo<X> { void something(@CW("X") Object x); }
  // new Foo<String>().something(123);
  @Nullable
  private RequiredType resolveTypeFromClass(
      Type calledType, ClassSymbol clazzSymbol, String typeArgName, VisitorState state) {
    // Try on the class
    int tyargIndex = findTypeArgInList(clazzSymbol, typeArgName);
    if (tyargIndex != -1) {
      return RequiredType.create(
          extractTypeArgAsMemberOfSupertype(calledType, clazzSymbol, tyargIndex, state.getTypes()));
    }

    while (clazzSymbol.isInner()) {
      // class Foo<T> {
      //    class Bar {
      //      void something(@CW("T") Object o));
      //    }
      // }
      // new Foo<String>().new Bar().something(123); // should fail, 123 needs to match String
      ClassSymbol encloser = clazzSymbol.owner.enclClass();
      calledType = calledType.getEnclosingType();
      tyargIndex = findTypeArgInList(encloser, typeArgName);
      if (tyargIndex != -1) {
        if (calledType.getTypeArguments().isEmpty()) {
          // If the receiver is held in a reference without the enclosing class's type arguments, we
          // can't determine the required type:
          // new Foo<String>().new Bar().something(123); // Yep
          // Foo<String>.Bar bar = ...;
          // bar.something(123); // Yep
          // Foo.Bar bar = ...;
          // bar.something(123); // Nope (this call would be unchecked if arg was T)
          return null;
        }
        return RequiredType.create(calledType.getTypeArguments().get(tyargIndex));
      }
      clazzSymbol = encloser;
    }
    return null;
  }

  private static int findTypeArgInList(Parameterizable hasTypeParams, String typeArgName) {
    List<? extends TypeParameterElement> typeParameters = hasTypeParams.getTypeParameters();
    for (int i = 0; i < typeParameters.size(); i++) {
      if (typeParameters.get(i).getSimpleName().contentEquals(typeArgName)) {
        return i;
      }
    }
    return -1;
  }
}
