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
import static com.sun.source.tree.Tree.Kind.MEMBER_SELECT;
import static com.sun.source.tree.Tree.Kind.METHOD_INVOCATION;
import static java.lang.String.format;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
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

    MethodInvocationTree maybeThatCall = findThatCall(state);
    if (maybeThatCall == null || maybeThatCall.getArguments().size() != 1) {
      return NO_MATCH;
    }

    ExpressionTree arg = getOnlyElement(maybeThatCall.getArguments());
    String checkDescription = makeCheckDescription(arg, state);
    if (checkDescription == null) {
      /*
       * TODO(cpovirk): Consider alternative ways of guessing the name.
       *
       * Our best bet: Look at the name of the assertion method. If the method is `StringSubject
       * hasUsername()`, then we should probably generate `check("username()")`. Similarly for
       * `StringSubject withUsername()` and `StringSubject username()`.
       *
       * One bit of complexity here is parameters: If the method is `StringSubject
       * hasUsername(String param)`, should we generate `check("username()")` or
       * `check("username(%s)", param)`? The former is right if the assertion is
       * `check().that(actualUsername).isEqualTo(param)`; the latter is right if the assertion is
       * `return check().that(usernameFor(param))`. We could guess based on whether `param` is used
       * before the call to `that` (including in computing the argument to `that`, even though the
       * argument comes textually *after* `that`) and based on whether the assertion method returns
       * a Subject (though note that some methods `return this`, a practice we discourage).
       */
      return NO_MATCH;
    }

    String newCheck = format("check(%s)", checkDescription);
    if (maybeCheckCall.getMethodSelect().getKind() == IDENTIFIER) {
      return describeMatch(maybeCheckCall, replace(maybeCheckCall, newCheck));
    } else if (maybeCheckCall.getMethodSelect().getKind() == MEMBER_SELECT) {
      MemberSelectTree methodSelect = (MemberSelectTree) maybeCheckCall.getMethodSelect();
      return describeMatch(
          maybeCheckCall,
          replace(
              state.getEndPosition(methodSelect.getExpression()),
              state.getEndPosition(maybeCheckCall),
              "." + newCheck));
    } else {
      return NO_MATCH;
    }
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
          .onDescendantOf("com.google.common.truth.Subject")
          .named("check")
          .withParameters();

  private static final Matcher<ExpressionTree> SUBJECT_BUILDER_THAT =
      anyOf(
          instanceMethod()
              .onDescendantOf("com.google.common.truth.CustomSubjectBuilder")
              .named("that"),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.SimpleSubjectBuilder")
              .named("that"),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
              .named("that"));

  private static final Matcher<ExpressionTree> WITH_MESSAGE_OR_ABOUT =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
          .namedAnyOf("withMessage", "about");
}
