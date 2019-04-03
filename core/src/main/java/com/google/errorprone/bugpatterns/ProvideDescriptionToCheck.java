/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.bugpatterns.ImplementAssertionWithChaining.makeCheckDescription;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static com.sun.source.tree.Tree.Kind.METHOD_INVOCATION;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

/**
 * Migrates Truth subjects from the no-arg overload of {@code Subject.check()} to the overload that
 * accepts a description.
 *
 * <pre>{@code
 * // Before:
 * check().that(actual().foo()).isEqualTo(expected);
 *
 * // After:
 * check("foo()").that(actual().foo()).isEqualTo(expected);
 * }</pre>
 */
@BugPattern(
    name = "ProvideDescriptionToCheck",
    summary = "Provide a description of the value to be included in the failure message.",
    severity = SUGGESTION,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class ProvideDescriptionToCheck extends BugChecker
    implements MethodInvocationTreeMatcher {
  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree maybeCheckCall, VisitorState state) {
    if (!NO_ARG_CHECK.matches(maybeCheckCall, state)) {
      return NO_MATCH;
    }
    if (maybeCheckCall.getMethodSelect().getKind() != IDENTIFIER) {
      return NO_MATCH;
      // TODO(cpovirk): Handle "this.check(...)," etc.
    }

    MethodInvocationTree maybeThatCall = findThatCall(state);
    if (maybeThatCall == null) {
      return NO_MATCH;
    }

    ExpressionTree arg = getOnlyElement(maybeThatCall.getArguments());
    String checkDescription = makeCheckDescription(arg, state);
    if (checkDescription == null) {
      return NO_MATCH;
    }
    return describeMatch(
        maybeCheckCall, replace(maybeCheckCall, String.format("check(%s)", checkDescription)));
  }

  /**
   * Starting from a {@code VisitorState} pointing at part of a fluent assertion statement (like
   * {@code check()} or {@code assertWithMessage()}, walks up the tree and returns the subsequent
   * call to {@code that(...)}.
   *
   * <p>Often, the call is made directly on the result of the given tree -- like when the input is
   * {@code check()}, which is part of the expression {@code check().that(...)}. But sometimes there
   * is an intervening call to {@code withMessage}, {@code about}, or both.
   */
  static MethodInvocationTree findThatCall(VisitorState state) {
    TreePath path = state.getPath();
    /*
     * Each iteration walks 1 method call up the tree, but it's actually 2 steps in the tree because
     * there's a MethodSelectTree between each pair of MethodInvocationTrees.
     */
    while (true) {
      path = path.getParentPath().getParentPath();
      Tree leaf = path.getLeaf();
      if (leaf.getKind() != METHOD_INVOCATION) {
        return null;
      }
      MethodInvocationTree maybeThatCall = (MethodInvocationTree) leaf;
      if (WITH_MESSAGE_OR_ABOUT.matches(maybeThatCall, state)) {
        continue;
      } else if (SUBJECT_BUILDER_THAT.matches(maybeThatCall, state)) {
        return maybeThatCall;
      } else {
        return null;
      }
    }
  }

  private static final Matcher<ExpressionTree> NO_ARG_CHECK =
      instanceMethod()
          .onExactClass("com.google.common.truth.Subject")
          .named("check")
          .withParameters();

  private static final Matcher<ExpressionTree> SUBJECT_BUILDER_THAT =
      anyOf(
          instanceMethod()
              .onExactClass("com.google.common.truth.CustomSubjectBuilder")
              .named("that"),
          instanceMethod()
              .onExactClass("com.google.common.truth.SimpleSubjectBuilder")
              .named("that"),
          instanceMethod()
              .onExactClass("com.google.common.truth.StandardSubjectBuilder")
              .named("that"));

  private static final Matcher<ExpressionTree> WITH_MESSAGE_OR_ABOUT =
      instanceMethod()
          .onExactClass("com.google.common.truth.StandardSubjectBuilder")
          .namedAnyOf("withMessage", "about");
}
