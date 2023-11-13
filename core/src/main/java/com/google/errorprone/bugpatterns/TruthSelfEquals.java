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
import static com.google.errorprone.util.ASTHelpers.sameVariable;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import javax.inject.Inject;

/**
 * Points out if an object is tested for equality/inequality to itself using Truth Libraries.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    summary =
        "isEqualTo should not be used to test an object for equality with itself; the"
            + " assertion will never fail.",
    severity = ERROR)
public final class TruthSelfEquals extends BugChecker implements MethodInvocationTreeMatcher {

  private final Matcher<MethodInvocationTree> equalsMatcher =
      allOf(
          instanceMethod().anyClass().namedAnyOf("isEqualTo", "isSameInstanceAs"),
          this::receiverSameAsParentsArgument);

  private final Matcher<MethodInvocationTree> notEqualsMatcher =
      allOf(
          instanceMethod().anyClass().namedAnyOf("isNotEqualTo", "isNotSameInstanceAs"),
          this::receiverSameAsParentsArgument);

  private final Matcher<MethodInvocationTree> otherMatcher =
      allOf(
          instanceMethod()
              .anyClass()
              .namedAnyOf("containsExactlyElementsIn", "containsAtLeastElementsIn", "areEqualTo"),
          this::receiverSameAsParentsArgument);

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
    Description.Builder description = buildDescription(tree);
    ExpressionTree toReplace = tree.getArguments().get(0);
    if (equalsMatcher.matches(tree, state)) {
      description
          .setMessage(generateSummary(getSymbol(tree).getSimpleName().toString(), "passes"))
          .addFix(suggestEqualsTesterFix(tree, toReplace, state));
    } else if (notEqualsMatcher.matches(tree, state)) {
      description.setMessage(generateSummary(getSymbol(tree).getSimpleName().toString(), "fails"));
    } else if (otherMatcher.matches(tree, state)) {
      description.setMessage(generateSummary(getSymbol(tree).getSimpleName().toString(), "passes"));
    } else {
      return NO_MATCH;
    }
    SuggestedFix fix = SelfEquals.fieldFix(toReplace, state);
    if (fix != null) {
      description.addFix(fix);
    }
    return description.build();
  }

  private static String generateSummary(String methodName, String constantOutput) {
    return "The arguments to the "
        + methodName
        + " method are the same object, so it always "
        + constantOutput
        + ". Please change the arguments to point to different objects or "
        + "consider using EqualsTester.";
  }

  private boolean receiverSameAsParentsArgument(MethodInvocationTree tree, VisitorState state) {
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
    if (sameVariable(receiverExpression, invocationExpression)) {
      return true;
    }
    var receiverConstant = constantExpressions.constantExpression(receiverExpression, state);
    var invocationConstant = constantExpressions.constantExpression(invocationExpression, state);
    return receiverConstant.isPresent() && receiverConstant.equals(invocationConstant);
  }

  private static SuggestedFix suggestEqualsTesterFix(
      MethodInvocationTree methodInvocationTree, ExpressionTree toReplace, VisitorState state) {
    String equalsTesterSuggest =
        "new EqualsTester().addEqualityGroup("
            + state.getSourceForNode(toReplace)
            + ").testEquals()";
    return SuggestedFix.builder()
        .replace(methodInvocationTree, equalsTesterSuggest)
        .addImport("com.google.common.testing.EqualsTester")
        .build();
  }
}
