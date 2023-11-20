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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.sameVariable;
import static java.lang.String.format;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import javax.inject.Inject;

/** A {@link BugPattern}; see the summary. */
// TODO(ghm): Rename to SelfAssertion or something.
@BugPattern(summary = "This assertion will always fail or succeed.", severity = ERROR)
public final class TruthSelfEquals extends BugChecker implements MethodInvocationTreeMatcher {

  private final Matcher<MethodInvocationTree> equalsMatcher =
      anyOf(
          allOf(
              instanceMethod()
                  .anyClass()
                  .namedAnyOf(
                      "isEqualTo",
                      "isSameInstanceAs",
                      "containsExactlyElementsIn",
                      "containsAtLeastElementsIn",
                      "areEqualTo"),
              this::truthSameArguments),
          allOf(
              staticMethod()
                  .onClassAny(
                      "org.junit.Assert", "junit.framework.Assert", "junit.framework.TestCase")
                  .namedAnyOf("assertEquals", "assertArrayEquals"),
              this::junitSameArguments));

  private final Matcher<MethodInvocationTree> notEqualsMatcher =
      anyOf(
          allOf(
              instanceMethod().anyClass().namedAnyOf("isNotEqualTo", "isNotSameInstanceAs"),
              this::truthSameArguments),
          allOf(
              staticMethod()
                  .onClassAny(
                      "org.junit.Assert", "junit.framework.Assert", "junit.framework.TestCase")
                  .namedAnyOf("assertNotEquals"),
              this::junitSameArguments));

  private static final Matcher<ExpressionTree> ASSERT_THAT =
      anyOf(
          staticMethod().anyClass().named("assertThat"),
          instanceMethod().onDescendantOf("com.google.common.truth.TestVerb").named("that"),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
              .named("that"));

  private final ConstantExpressions constantExpressions;

  @Inject
  TruthSelfEquals(ConstantExpressions constantExpressions) {
    this.constantExpressions = constantExpressions;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (tree.getArguments().isEmpty()) {
      return NO_MATCH;
    }
    if (equalsMatcher.matches(tree, state)) {
      return buildDescription(tree)
          .setMessage(generateSummary(getSymbol(tree).getSimpleName().toString(), "passes"))
          .build();
    }
    if (notEqualsMatcher.matches(tree, state)) {
      return buildDescription(tree)
          .setMessage(generateSummary(getSymbol(tree).getSimpleName().toString(), "fails"))
          .build();
    }
    return NO_MATCH;
  }

  private static String generateSummary(String methodName, String constantOutput) {
    return format(
        "You are passing identical arguments to the %s method, so this assertion always %s. THIS IS"
            + " LIKELY A BUG! If you are trying to test the correctness of an equals()"
            + " implementation, use EqualsTester instead.",
        methodName, constantOutput);
  }

  private boolean junitSameArguments(MethodInvocationTree tree, VisitorState state) {
    var arguments = tree.getArguments();
    if (arguments.isEmpty()) {
      return false;
    }
    var firstArgument = tree.getArguments().get(0);
    ExpressionTree expected;
    ExpressionTree actual;
    if (tree.getArguments().size() > 2
        && isSameType(getType(firstArgument), state.getSymtab().stringType, state)) {
      expected = arguments.get(1);
      actual = arguments.get(2);
    } else {
      expected = arguments.get(0);
      actual = arguments.get(1);
    }
    return sameExpression(state, expected, actual);
  }

  private boolean truthSameArguments(MethodInvocationTree tree, VisitorState state) {
    ExpressionTree rec = getReceiver(tree);
    if (rec == null) {
      return false;
    }
    if (!ASSERT_THAT.matches(rec, state)) {
      return false;
    }
    if (((MethodInvocationTree) rec).getArguments().size() != 1
        || tree.getArguments().size() != 1) {
      // Oops: we over-matched and this doesn't look like a normal assertion.
      return false;
    }
    ExpressionTree receiverExpression = getOnlyElement(((MethodInvocationTree) rec).getArguments());
    ExpressionTree invocationExpression = getOnlyElement(tree.getArguments());
    return sameExpression(state, receiverExpression, invocationExpression);
  }

  private boolean sameExpression(
      VisitorState state, ExpressionTree receiverExpression, ExpressionTree invocationExpression) {
    if (sameVariable(receiverExpression, invocationExpression)) {
      return true;
    }
    var receiverConstant = constantExpressions.constantExpression(receiverExpression, state);
    var invocationConstant = constantExpressions.constantExpression(invocationExpression, state);
    return receiverConstant.isPresent() && receiverConstant.equals(invocationConstant);
  }
}
