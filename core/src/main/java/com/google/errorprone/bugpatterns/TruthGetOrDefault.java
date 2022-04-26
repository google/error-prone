/*
 * Copyright 2020 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.Objects;

/**
 * Flags ambiguous usages of {@code Map#getOrDefault} within {@code Truth#assertThat}.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    summary = "Asserting on getOrDefault is unclear; prefer containsEntry or doesNotContainKey",
    severity = WARNING)
public final class TruthGetOrDefault extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> ASSERT_THAT =
      staticMethod().onClass("com.google.common.truth.Truth").named("assertThat");

  private static final Matcher<ExpressionTree> GET_OR_DEFAULT_MATCHER =
      instanceMethod().onDescendantOf("java.util.Map").named("getOrDefault");

  private static final Matcher<ExpressionTree> SUBJECT_EQUALS_MATCHER =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.Subject")
          .named("isEqualTo")
          .withParameters("java.lang.Object");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!SUBJECT_EQUALS_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    ExpressionTree rec = ASTHelpers.getReceiver(tree);
    if (rec == null) {
      return Description.NO_MATCH;
    }
    if (!ASSERT_THAT.matches(rec, state)) {
      return Description.NO_MATCH;
    }
    ExpressionTree arg = getOnlyElement(((MethodInvocationTree) rec).getArguments());
    if (!GET_OR_DEFAULT_MATCHER.matches(arg, state)) {
      return Description.NO_MATCH;
    }
    MethodInvocationTree argMethodInvocationTree = (MethodInvocationTree) arg;
    ExpressionTree defaultVal = argMethodInvocationTree.getArguments().get(1);
    ExpressionTree expectedVal = getOnlyElement(tree.getArguments());
    Match match = areValuesSame(defaultVal, expectedVal, state);
    switch (match) {
      case UNKNOWN:
        return Description.NO_MATCH;
      case DIFFERENT:
        return describeMatch(
            tree,
            SuggestedFix.builder()
                .replace(
                    argMethodInvocationTree,
                    state.getSourceForNode(ASTHelpers.getReceiver(argMethodInvocationTree)))
                .replace(
                    state.getEndPosition(rec),
                    state.getEndPosition(tree.getMethodSelect()),
                    ".containsEntry")
                .replace(
                    tree.getArguments().get(0),
                    state.getSourceForNode(argMethodInvocationTree.getArguments().get(0))
                        + ", "
                        + state.getSourceForNode(tree.getArguments().get(0)))
                .build());
      case SAME:
        return describeMatch(
            tree,
            SuggestedFix.builder()
                .replace(arg, state.getSourceForNode(ASTHelpers.getReceiver(arg)))
                .replace(
                    state.getEndPosition(rec),
                    state.getEndPosition(tree.getMethodSelect()),
                    ".doesNotContainKey")
                .replace(
                    tree.getArguments().get(0),
                    state.getSourceForNode(argMethodInvocationTree.getArguments().get(0)))
                .build());
    }
    return Description.NO_MATCH;
  }

  private enum Match {
    SAME,
    DIFFERENT,
    UNKNOWN;
  }

  /**
   * Returns {@code Match.SAME} or {@code Match.DIFFERENT} when it can confidently say that both
   * expressions are same or different, otherwise returns {@code Match.UNKNOWN}.
   */
  private static Match areValuesSame(
      ExpressionTree defaultVal, ExpressionTree expectedVal, VisitorState state) {
    if (ASTHelpers.sameVariable(defaultVal, expectedVal)) {
      return Match.SAME;
    }
    Object expectedConstVal = ASTHelpers.constValue(expectedVal);
    Object defaultConstVal = ASTHelpers.constValue(defaultVal);
    if (expectedConstVal == null || defaultConstVal == null) {
      return Match.UNKNOWN;
    }
    if (Objects.equals(defaultConstVal, expectedConstVal)) {
      return Match.SAME;
    }
    if (!state
        .getTypes()
        .isSameType(ASTHelpers.getType(expectedVal), ASTHelpers.getType(defaultVal))) {
      return Match.UNKNOWN;
    }
    return Match.DIFFERENT;
  }
}
