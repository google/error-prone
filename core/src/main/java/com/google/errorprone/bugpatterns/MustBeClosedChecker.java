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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodIsConstructor;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers.ConstructorMatcher;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;

/**
 * Checks if a constructor or method annotated with {@link MustBeClosed} is called within the
 * resource variable initializer of a try-with-resources statement.
 */
@BugPattern(
    name = "MustBeClosedChecker",
    summary = "The result of this method must be closed.",
    category = JDK,
    severity = ERROR,
    generateExamplesFromTestCases = false)
public class MustBeClosedChecker extends AbstractMustBeClosedChecker
    implements MethodTreeMatcher, MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final Matcher<MethodTree> METHOD_RETURNS_AUTO_CLOSEABLE_MATCHER =
      allOf(not(methodIsConstructor()), methodReturns(isSubtypeOf("java.lang.AutoCloseable")));

  private static final Matcher<MethodTree> AUTO_CLOSEABLE_CONSTRUCTOR_MATCHER =
      allOf(methodIsConstructor(), enclosingClass(isSubtypeOf("java.lang.AutoCloseable")));
  public static final ConstructorMatcher CONSTRUCTOR = constructor();

  /**
   * Check that the {@link MustBeClosed} annotation is only used for constructors of AutoCloseables
   * and methods that return an AutoCloseable.
   */
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!HAS_MUST_BE_CLOSED_ANNOTATION.matches(tree, state)) {
      // Ignore methods and constructors that are not annotated with {@link MustBeClosed}.
      return NO_MATCH;
    }

    boolean isAConstructor = methodIsConstructor().matches(tree, state);
    if (isAConstructor && !AUTO_CLOSEABLE_CONSTRUCTOR_MATCHER.matches(tree, state)) {
      return buildDescription(tree)
          .setMessage("MustBeClosed should only annotate constructors of AutoCloseables.")
          .build();
    }

    if (!isAConstructor && !METHOD_RETURNS_AUTO_CLOSEABLE_MATCHER.matches(tree, state)) {
      return buildDescription(tree)
          .setMessage("MustBeClosed should only annotate methods that return an AutoCloseable.")
          .build();
    }
    return NO_MATCH;
  }

  /**
   * Check that invocations of methods annotated with {@link MustBeClosed} are called within the
   * resource variable initializer of a try-with-resources statement.
   */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!HAS_MUST_BE_CLOSED_ANNOTATION.matches(tree, state)) {
      return NO_MATCH;
    }
    if (CONSTRUCTOR.matches(tree, state)) {
      return NO_MATCH;
    }
    return matchNewClassOrMethodInvocation(tree, state);
  }

  /**
   * Check that construction of constructors annotated with {@link MustBeClosed} occurs within the
   * resource variable initializer of a try-with-resources statement.
   */
  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!HAS_MUST_BE_CLOSED_ANNOTATION.matches(tree, state)) {
      return NO_MATCH;
    }
    return matchNewClassOrMethodInvocation(tree, state);
  }
}
