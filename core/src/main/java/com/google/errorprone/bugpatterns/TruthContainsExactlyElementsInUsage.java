/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import java.util.Optional;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    tags = {StandardTags.REFACTORING},
    summary =
        "containsExactly is preferred over containsExactlyElementsIn when creating new iterables.",
    severity = WARNING)
public final class TruthContainsExactlyElementsInUsage extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> CONTAINS_EXACTLY_ELEMENTS_IN_METHOD_MATCHER =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.IterableSubject")
          .named("containsExactlyElementsIn");

  // Not including Sets for the rare occasion of duplicate element declarations inside the set. If
  // refactored to containsExactly, the behavior is different.
  private static final Matcher<ExpressionTree> NEW_ITERABLE_MATCHERS =
      anyOf(
          staticMethod().onClass("com.google.common.collect.Lists").named("newArrayList"),
          staticMethod().onClass("com.google.common.collect.ImmutableList").named("of"),
          staticMethod().onClass("java.util.Arrays").named("asList"),
          staticMethod().onClass("java.util.Collections").named("singletonList"),
          staticMethod().onClass("java.util.List").named("of"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!CONTAINS_EXACTLY_ELEMENTS_IN_METHOD_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    // Avoids refactoring variables and method invocations that are not creating new iterables.
    // The first param from containsExactlyElementsIn should always be an Iterable.
    return getArgumentsFromNewIterableExpression(tree.getArguments().get(0), state)
        .map(arguments -> describeMatch(tree, refactor(arguments, tree, state)))
        .orElse(Description.NO_MATCH);
  }

  // Returns the arguments from the expression. If it is not a valid expression, returns empty.
  private static Optional<ImmutableList<ExpressionTree>> getArgumentsFromNewIterableExpression(
      ExpressionTree expression, VisitorState state) {
    if (expression instanceof MethodInvocationTree
        && NEW_ITERABLE_MATCHERS.matches(expression, state)) {
      MethodInvocationTree paramMethodInvocationTree = (MethodInvocationTree) expression;
      return Optional.of(ImmutableList.copyOf(paramMethodInvocationTree.getArguments()));
    } else if (expression instanceof NewArrayTree) {
      NewArrayTree newArrayTree = (NewArrayTree) expression;
      return Optional.of(ImmutableList.copyOf(newArrayTree.getInitializers()));
    }
    return Optional.empty();
  }

  private static SuggestedFix refactor(
      ImmutableList<ExpressionTree> arguments, MethodInvocationTree tree, VisitorState state) {
    // First we replace the containsExactlyElementsIn method with containsExactly.
    SuggestedFix.Builder fix =
        SuggestedFix.builder()
            .merge(SuggestedFixes.renameMethodInvocation(tree, "containsExactly", state));
    // Finally, we use the arguments from the new iterable to build the containsExactly arguments.
    ExpressionTree expressionToReplace = tree.getArguments().get(0);
    if (!arguments.isEmpty()) {
      fix.replace(getStartPosition(expressionToReplace), getStartPosition(arguments.get(0)), "")
          .replace(
              state.getEndPosition(getLast(arguments)),
              state.getEndPosition(expressionToReplace),
              "");
    } else {
      fix.delete(expressionToReplace);
    }
    return fix.build();
  }
}
