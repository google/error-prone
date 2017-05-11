/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.classLiteral;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.sun.tools.javac.code.TypeTag.BOT;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.google.common.util.concurrent.Futures;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;

/**
 * Checks for calls to Guava's {@code Futures.getChecked} method that will always fail because they
 * pass an incompatible exception type.
 */
@BugPattern(
  name = "FuturesGetCheckedIllegalExceptionType",
  summary = "Futures.getChecked requires a checked exception type with a standard constructor.",
  explanation =
      "The passed exception type must not be a RuntimeException, and it must expose a "
          + "public constructor whose only parameters are of type String or Throwable. getChecked "
          + "will reject any other type with an IllegalArgumentException.",
  category = GUAVA,
  severity = ERROR
)
public final class FuturesGetCheckedIllegalExceptionType extends BugChecker
    implements MethodInvocationTreeMatcher {
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!FUTURES_GET_CHECKED_MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }

    /*
     * Check for RuntimeException first: There would be no sense in telling the user that the
     * problem is related to which constructors are available if we'd reject the call anyway.
     */
    if (PASSED_RUNTIME_EXCEPTION_TYPE.matches(tree, state)) {
      return describeUncheckedExceptionTypeMatch(
          tree,
          SuggestedFix.builder()
              .replace(tree, "getUnchecked(" + tree.getArguments().get(0) + ")")
              .addStaticImport(Futures.class.getName() + ".getUnchecked")
              .build());
    }

    if (PASSED_TYPE_WITHOUT_USABLE_CONSTRUCTOR.matches(tree, state)) {
      return describeNoValidConstructorMatch(tree);
    }

    return NO_MATCH;
  }

  private static final Matcher<ExpressionTree> FUTURES_GET_CHECKED_MATCHER =
      anyOf(
          staticMethod().onClass(Futures.class.getName()).named("getChecked"));

  private static final Matcher<ExpressionTree> CLASS_OBJECT_FOR_CLASS_EXTENDING_RUNTIME_EXCEPTION =
      new Matcher<ExpressionTree>() {
        @Override
        public boolean matches(ExpressionTree tree, VisitorState state) {
          Types types = state.getTypes();
          Type classType = state.getSymtab().classType;
          Type runtimeExceptionType = state.getSymtab().runtimeExceptionType;
          Type argType = getType(tree);

          // Make sure that the argument is a Class<Something> (and not null/bottom).
          if (!isSubtype(argType, classType, state) || argType.getTag() == BOT) {
            return false;
          }

          Type exceptionType = ((ClassType) argType).getTypeArguments().head;
          return types.isSubtype(exceptionType, runtimeExceptionType);
        }
      };

  private static final Matcher<MethodInvocationTree> PASSED_RUNTIME_EXCEPTION_TYPE =
      argument(1, CLASS_OBJECT_FOR_CLASS_EXTENDING_RUNTIME_EXCEPTION);

  private static final Matcher<ExpressionTree> CLASS_OBJECT_FOR_CLASS_WITHOUT_USABLE_CONSTRUCTOR =
      classLiteral(
          new Matcher<ExpressionTree>() {
            @Override
            public boolean matches(ExpressionTree tree, VisitorState state) {
              ClassSymbol classSymbol = (ClassSymbol) getSymbol(tree);
              if (classSymbol == null) {
                return false;
              }

              if (classSymbol.isInner()) {
                return true;
              }

              for (Symbol enclosedSymbol : classSymbol.getEnclosedElements()) {
                if (!enclosedSymbol.isConstructor()) {
                  continue;
                }
                MethodSymbol constructorSymbol = (MethodSymbol) enclosedSymbol;
                if (canBeUsedByGetChecked(constructorSymbol, state)) {
                  return false;
                }
              }

              return true;
            }
          });

  private static final Matcher<MethodInvocationTree> PASSED_TYPE_WITHOUT_USABLE_CONSTRUCTOR =
      argument(1, CLASS_OBJECT_FOR_CLASS_WITHOUT_USABLE_CONSTRUCTOR);

  private static boolean canBeUsedByGetChecked(MethodSymbol constructor, VisitorState state) {
    Type stringType = state.getSymtab().stringType;
    Type throwableType = state.getSymtab().throwableType;

    // TODO(cpovirk): Check visibility of enclosing types (assuming that it matters to getChecked).
    if (!constructor.getModifiers().contains(PUBLIC)) {
      return false;
    }

    for (VarSymbol param : constructor.getParameters()) {
      if (!isSameType(param.asType(), stringType, state)
          && !isSameType(param.asType(), throwableType, state)) {
        return false;
      }
    }

    return true;
  }

  private Description describeUncheckedExceptionTypeMatch(Tree tree, Fix fix) {
    return buildDescription(tree)
        .setMessage(
            "The exception class passed to getChecked must be a checked exception, "
                + "not a RuntimeException.")
        .addFix(fix)
        .build();
  }

  private Description describeNoValidConstructorMatch(Tree tree) {
    return buildDescription(tree)
        .setMessage(
            "The exception class passed to getChecked must declare a public constructor whose "
                + "only parameters are of type String or Throwable.")
        .build();
  }
}
