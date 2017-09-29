/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.android;

import static com.google.errorprone.BugPattern.Category.ANDROID;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.base.Preconditions;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.LambdaExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import javax.lang.model.element.ElementKind;

/**
 * Error-prone enforcer that prevents use of internal support library APIs.
 *
 * @author clm@google.com (Charles Munger)
 */
@BugPattern(
  name = "RestrictTo",
  summary = "Use of method or class annotated with @RestrictTo",
  category = ANDROID,
  severity = ERROR
)
public final class RestrictToEnforcer extends BugChecker
    implements AnnotationTreeMatcher,
        LambdaExpressionTreeMatcher,
        MemberReferenceTreeMatcher,
        MethodInvocationTreeMatcher,
        MethodTreeMatcher,
        NewClassTreeMatcher,
        IdentifierTreeMatcher {

  @Override
  public final Description matchAnnotation(AnnotationTree tree, VisitorState state) {
    Symbol symbol = ASTHelpers.getSymbol(tree);
    if (!compilingSupportLibrary(state)
        && symbol.flatName().contentEquals("android.support.annotation.RestrictTo")) {
      return buildDescription(tree)
          .setMessage("@RestrictTo cannot be used outside the support library")
          .build();
    }
    return Description.NO_MATCH;
  }

  @Override
  public final Description matchIdentifier(IdentifierTree tree, VisitorState state) {
    if (compilingSupportLibrary(state)) {
      return Description.NO_MATCH;
    }
    Symbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol instanceof TypeSymbol) {
      TypeSymbol typeSymbol = (TypeSymbol) symbol;
      if (checkEnclosingClasses(typeSymbol, state)) {
        return describe(tree, symbol.enclClass(), state);
      }
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchLambdaExpression(LambdaExpressionTree tree, VisitorState state) {
    if (compilingSupportLibrary(state)) {
      return Description.NO_MATCH;
    }
    Type lambdaType = ASTHelpers.getUpperBound(ASTHelpers.getType(tree), state.getTypes());
    if (checkEnclosingTypes(lambdaType, state)) {
      return describe(tree, lambdaType.asElement().enclClass(), state);
    }

    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (compilingSupportLibrary(state)) {
      return Description.NO_MATCH;
    }
    MethodSymbol method = ASTHelpers.getSymbol(tree);

    // Directly calling a method that's restricted, or is declared on a restricted class
    return matchInvokedMethod(tree, method, state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (compilingSupportLibrary(state)) {
      return Description.NO_MATCH;
    }

    MethodSymbol method = ASTHelpers.getSymbol(tree);
    // Directly calling a method that's restricted, or is declared on a restricted class
    return matchInvokedMethod(tree, method, state);
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    if (compilingSupportLibrary(state)) {
      return Description.NO_MATCH;
    }

    Type refType = ASTHelpers.getUpperBound(ASTHelpers.getResultType(tree), state.getTypes());
    if (checkEnclosingTypes(refType, state)) {
      return describe(tree, refType.asElement().enclClass(), state);
    }

    Symbol symbol = ASTHelpers.getSymbol(tree);
    if (!(symbol instanceof MethodSymbol)) {
      return Description.NO_MATCH;
    }
    MethodSymbol method = (MethodSymbol) symbol;

    // Directly calling a method that's restricted, or is declared on a restricted class
    return matchInvokedMethod(tree, method, state);
  }

  private Description matchInvokedMethod(
      ExpressionTree tree, MethodSymbol method, VisitorState state) {
    if (methodIsRestricted(method, state)) {
      return describe(tree, method, state);
    }
    if (method.getKind() == ElementKind.CONSTRUCTOR) {
      if (checkEnclosingClasses(method, state)) {
        return describe(tree, method, state);
      }
    } else {
      Type receiverType =
          ASTHelpers.getUpperBound(ASTHelpers.getReceiverType(tree), state.getTypes());
      if (checkEnclosingTypes(receiverType, state)) {
        return describe(tree, receiverType.asElement().enclClass(), method, state);
      }
    }
    // Calling a method whose super declaration is restricted
    return matchMethodSymbol(tree, method, state);
  }

  private Description matchMethodSymbol(Tree tree, MethodSymbol method, VisitorState state) {
    for (MethodSymbol superSymbol : ASTHelpers.findSuperMethods(method, state.getTypes())) {
      if (methodIsRestricted(superSymbol, state)) {
        return describe(tree, superSymbol, state);
      }
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (compilingSupportLibrary(state)) {
      return Description.NO_MATCH;
    }
    MethodSymbol method = ASTHelpers.getSymbol(tree);

    /* We're allowed to override a method if all of:
     * 1. The class we're extending is not restricted
     * 2. No class in our closure has this method annotated as @RestrictTo
     */
    if (checkEnclosingClasses(method, state)) {
      return describe(tree, method, state);
    }
    for (Type type : state.getTypes().closure(ASTHelpers.enclosingClass(method).asType())) {
      MethodSymbol superSymbol = ASTHelpers.findSuperMethodInType(method, type, state.getTypes());
      if (superSymbol != null && methodIsRestricted(superSymbol, state)) {
        return describe(tree, superSymbol, state);
      }
    }
    return Description.NO_MATCH;
  }

  private Description describe(Tree tree, MethodSymbol method, VisitorState state) {
    return describe(tree, ASTHelpers.enclosingClass(method), method, state);
  }

  private Description describe(
      Tree tree, ClassSymbol classSym, MethodSymbol method, VisitorState state) {
    return describe(
        tree,
        String.format(
            "Method %s.%s is restricted, not for use outside the support library.",
            classSym, Signatures.prettyMethodSignature(ASTHelpers.enclosingClass(method), method)),
        state);
  }

  private Description describe(Tree tree, String message, VisitorState state) {
    return buildDescription(tree).setMessage(message).build();
  }

  private Description describe(Tree tree, ClassSymbol classSym, VisitorState state) {
    return describe(
        tree,
        String.format("Class %s is restricted, not for use outside the support library.", classSym),
        state);
  }

  private static boolean methodIsRestricted(MethodSymbol method, VisitorState state) {
    return symbolInSupportLibrary(method) && hasRestrictedAnnotation(method, state);
  }

  private static boolean symbolInSupportLibrary(Symbol sym) {
    return ASTHelpers.enclosingPackage(sym)
        .getQualifiedName()
        .toString()
        .startsWith("android.support");
  }

  private static boolean compilingSupportLibrary(VisitorState state) {
    ExpressionTree tree = state.getPath().getCompilationUnit().getPackageName();
    return tree != null && tree.toString().startsWith("android.support");
  }

  private static boolean checkEnclosingTypes(Type type, VisitorState state) {
    ClassSymbol clazz = type.asElement().enclClass();
    Preconditions.checkNotNull(clazz, "Type %s has no enclosing class", type);
    return checkEnclosingClasses(clazz, state);
  }

  private static boolean checkEnclosingClasses(Symbol symbol, VisitorState state) {
    if (!symbolInSupportLibrary(symbol)) {
      return false;
    }
    for (Symbol enclosingClass = symbol;
        enclosingClass != null;
        enclosingClass = enclosingClass.owner.enclClass()) {
      if (hasRestrictedAnnotation(enclosingClass, state)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasRestrictedAnnotation(Symbol sym, VisitorState state) {
    return ASTHelpers.hasAnnotation(sym, "android.support.annotation.RestrictTo", state);
  }
}
