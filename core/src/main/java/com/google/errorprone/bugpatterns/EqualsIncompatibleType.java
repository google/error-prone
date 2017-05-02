/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.suppliers.Suppliers.BOOLEAN_TYPE;

import com.google.common.base.Predicate;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Name;
import java.util.Set;

/** @author avenet@google.com (Arnaud J. Venet) */
@BugPattern(
  name = "EqualsIncompatibleType",
  summary = "An equality test between objects with incompatible types always returns false",
  category = JDK,
  severity = WARNING
)
public class EqualsIncompatibleType extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<MethodInvocationTree> STATIC_EQUALS_INVOCATION_MATCHER =
      anyOf(
          allOf(
              staticMethod()
                  .onClass("java.util.Objects")
                  .named("equals")
                  .withParameters("java.lang.Object", "java.lang.Object"),
              isSameType(BOOLEAN_TYPE)),
          allOf(
              staticMethod()
                  .onClass("com.google.common.base.Objects")
                  .named("equal")
                  .withParameters("java.lang.Object", "java.lang.Object"),
              isSameType(BOOLEAN_TYPE)));

  private static final Matcher<MethodInvocationTree> INSTANCE_EQUALS_INVOCATION_MATCHER =
      allOf(
          instanceMethod().anyClass().named("equals").withParameters("java.lang.Object"),
          isSameType(BOOLEAN_TYPE));

  private static final Matcher<Tree> ASSERT_FALSE_MATCHER =
      toType(
          MethodInvocationTree.class,
          anyOf(
              instanceMethod().anyClass().named("assertFalse"),
              staticMethod().anyClass().named("assertFalse")));

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree invocationTree, final VisitorState state) {
    if (!STATIC_EQUALS_INVOCATION_MATCHER.matches(invocationTree, state)
        && !INSTANCE_EQUALS_INVOCATION_MATCHER.matches(invocationTree, state)) {
      return Description.NO_MATCH;
    }

    // This is the type of the object on which the java.lang.Object.equals() method
    // is called, either directly or indirectly via a static utility method. In the latter,
    // it is the type of the first argument to the static method.
    Type receiverType;
    // This is the type of the argument to the java.lang.Object.equals() method.
    // In case a static utility method is used, it is the type of the second argument
    // to this method.
    Type argumentType;

    if (STATIC_EQUALS_INVOCATION_MATCHER.matches(invocationTree, state)) {
      receiverType = ASTHelpers.getType(invocationTree.getArguments().get(0));
      argumentType = ASTHelpers.getType(invocationTree.getArguments().get(1));
    } else {
      receiverType = ASTHelpers.getReceiverType(invocationTree);
      argumentType = ASTHelpers.getType(invocationTree.getArguments().get(0));
    }

    if (!incompatibleTypes(receiverType, argumentType, state)) {
      return Description.NO_MATCH;
    }

    // Ignore callsites wrapped inside assertFalse:
    // assertFalse(objOfReceiverType.equals(objOfArgumentType))
    if (ASSERT_FALSE_MATCHER.matches(state.getPath().getParentPath().getLeaf(), state)) {
      return Description.NO_MATCH;
    }

    // When we reach this point, we know that the two following facts hold:
    // (1) The types of the receiver and the argument to the eventual invocation of
    //     java.lang.Object.equals() are incompatible.
    // (2) No common superclass (other than java.lang.Object) or interface of the receiver and the
    //     argument defines an override of java.lang.Object.equals().
    // This equality test almost certainly evaluates to false, which is very unlikely to be the
    // programmer's intent. Hence, this is reported as an error. There is no sensible fix to suggest
    // in this situation.
    return buildDescription(invocationTree)
        .setMessage(getMessage(invocationTree, receiverType, argumentType))
        .build();
  }

  static boolean incompatibleTypes(Type receiverType, Type argumentType, final VisitorState state) {
    if (receiverType == null || argumentType == null) {
      return false;
    }

    // If one type can be cast into the other, we don't flag the equality test.
    // Note: we do this precisely in this order to allow primitive values to be checked pre-1.7:
    // 1.6: java.lang.Object can't be cast to primitives
    // 1.7: java.lang.Object can be cast to primitives (implicitly through the boxed primitive type)
    if (ASTHelpers.isCastable(argumentType, receiverType, state)) {
      return false;
    }

    // Otherwise, we explore the superclasses of the receiver type as well as the interfaces it
    // implements and we collect all overrides of java.lang.Object.equals(). If one of those
    // overrides is inherited by the argument, then we don't flag the equality test.
    final Types types = state.getTypes();
    Predicate<MethodSymbol> equalsPredicate =
        new Predicate<MethodSymbol>() {
          @Override
          public boolean apply(MethodSymbol methodSymbol) {
            return !methodSymbol.isStatic()
                && ((methodSymbol.flags() & Flags.SYNTHETIC) == 0)
                && types.isSameType(methodSymbol.getReturnType(), state.getSymtab().booleanType)
                && methodSymbol.getParameters().size() == 1
                && types.isSameType(
                    methodSymbol.getParameters().get(0).type, state.getSymtab().objectType);
          }
        };
    Name equalsName = state.getName("equals");
    Set<MethodSymbol> overridesOfEquals =
        ASTHelpers.findMatchingMethods(equalsName, equalsPredicate, receiverType, types);
    ClassSymbol argumentClass = (ClassSymbol) argumentType.tsym;

    for (MethodSymbol method : overridesOfEquals) {
      ClassSymbol methodClass = method.enclClass();
      if (argumentClass.isSubClass(methodClass, types)
          && !methodClass.equals(state.getSymtab().objectType.tsym)
          && !methodClass.equals(state.getSymtab().enumSym)) {
        // The type of the argument shares a superclass
        // (other then java.lang.Object or java.lang.Enum) or interface
        // with the receiver that implements an override of java.lang.Object.equals().
        return false;
      }
    }
    return true;
  }

  private static String getMessage(
      MethodInvocationTree invocationTree, Type receiverType, Type argumentType) {
    String receiverTypeString = Signatures.prettyType(receiverType);
    String argumentTypeString = Signatures.prettyType(argumentType);
    if (argumentTypeString.equals(receiverTypeString)) {
      receiverTypeString = receiverType.toString();
      argumentTypeString = argumentType.toString();
    }
    return "Calling "
        + ASTHelpers.getSymbol(invocationTree).getSimpleName()
        + " on incompatible types "
        + receiverTypeString
        + " and "
        + argumentTypeString;
  }
}
