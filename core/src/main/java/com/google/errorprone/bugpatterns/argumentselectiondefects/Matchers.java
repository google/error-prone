/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingMethod;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasArguments;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.ChildMultiMatcher.MatchType;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Matchers for the various checkers in this package. They are factored out into this class so that
 * {@code ArgumentSelectionDefectChecker} can avoid reporting duplicate findings that the other
 * checkers would have found
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
class Matchers {

  /** Matches if the tree is a constructor for an AutoValue class. */
  static final Matcher<NewClassTree> AUTOVALUE_CONSTRUCTOR =
      new Matcher<NewClassTree>() {
        @Override
        public boolean matches(NewClassTree tree, VisitorState state) {
          MethodSymbol sym = ASTHelpers.getSymbol(tree);
          if (sym == null) {
            return false;
          }

          ClassSymbol owner = (ClassSymbol) sym.owner;
          if (owner == null) {
            return false;
          }

          Type superType = owner.getSuperclass();
          if (superType == null) {
            return false;
          }

          Symbol superSymbol = superType.tsym;
          if (superSymbol == null) {
            return false;
          }

          if (!ASTHelpers.hasDirectAnnotationWithSimpleName(superSymbol, "AutoValue")) {
            return false;
          }

          return true;
        }
      };

  // if any of the arguments are instances of throwable then abort - people like to use
  // 'expected' as the name of the exception they are expecting
  private static final Matcher<MethodInvocationTree> ARGUMENT_EXTENDS_TRHOWABLE =
      hasArguments(MatchType.AT_LEAST_ONE, isSubtypeOf(Throwable.class));

  // if the method is a refaster-before template then it might be explicitly matching bad behaviour
  private static final Matcher<MethodInvocationTree> METHOD_ANNOTATED_WITH_BEFORETEMPLATE =
      enclosingMethod(hasAnnotation("com.google.errorprone.refaster.annotation.BeforeTemplate"));

  private static final Matcher<MethodInvocationTree> TWO_PARAMETER_ASSERT =
      new Matcher<MethodInvocationTree>() {
        @Override
        public boolean matches(MethodInvocationTree tree, VisitorState state) {
          List<VarSymbol> parameters = ASTHelpers.getSymbol(tree).getParameters();
          if (parameters.size() != 2) {
            return false;
          }
          return ASTHelpers.isSameType(
              parameters.get(0).asType(), parameters.get(1).asType(), state);
        }
      };

  private static final Matcher<MethodInvocationTree> THREE_PARAMETER_ASSERT =
      new Matcher<MethodInvocationTree>() {
        @Override
        public boolean matches(MethodInvocationTree tree, VisitorState state) {
          List<VarSymbol> parameters = ASTHelpers.getSymbol(tree).getParameters();
          if (parameters.size() != 3) {
            return false;
          }
          return ASTHelpers.isSameType(
                  parameters.get(0).asType(), state.getSymtab().stringType, state)
              && ASTHelpers.isSameType(
                  parameters.get(1).asType(), parameters.get(2).asType(), state);
        }
      };

  /** Matches if the tree corresponds to an assertEquals-style method */
  static final Matcher<MethodInvocationTree> ASSERT_METHOD =
      allOf(
          staticMethod()
              .onClassAny(
                  "org.junit.Assert",
                  "junit.framework.TestCase",
                  "junit.framework.Assert",
                  /* this final case is to allow testing without using the junit classes. we need to
                  do this because the junit dependency might not have been compiled with parameters
                  information which would cause the tests to fail.*/
                  "ErrorProneTest")
              .withNameMatching(Pattern.compile("assert.*")),
          anyOf(TWO_PARAMETER_ASSERT, THREE_PARAMETER_ASSERT),
          not(ARGUMENT_EXTENDS_TRHOWABLE),
          not(METHOD_ANNOTATED_WITH_BEFORETEMPLATE));
}
