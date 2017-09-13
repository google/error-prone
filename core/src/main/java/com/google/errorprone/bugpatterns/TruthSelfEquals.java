/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.Category.TRUTH;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.regex.Pattern;

/**
 * Points out if an object is tested for equality/inequality to itself using Truth Libraries.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
  name = "TruthSelfEquals",
  summary =
      "isEqualTo should not be used to test an object for equality with itself; the"
          + " assertion will never fail.",
  category = TRUTH,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class TruthSelfEquals extends BugChecker implements MethodInvocationTreeMatcher {

  /**
   * Matches calls to any Truth method called "isEqualTo"/"isNotEqualTo" with exactly one argument
   * in which the receiver is the same reference as the argument.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>assertThat(a).isEqualTo(a)
   *   <li>assertThat(a).isNotEqualTo(a)
   *   <li>assertThat(a).isNotSameAs(a)
   *   <li>assertThat(a).isSameAs(a)
   *   <li>assertWithMessage(msg).that(a).isEqualTo(a)
   *   <li>assertWithMessage(msg).that(a).isNotEqualTo(a)
   *   <li>assertWithMessage(msg).that(a).isNotSameAs(a)
   *   <li>assertWithMessage(msg).that(a).isSameAs(a)
   * </ul>
   */
  private static final Pattern EQUALS_SAME = Pattern.compile("(isEqualTo|isSameAs)");

  private static final Pattern NOT_EQUALS_NOT_SAME = Pattern.compile("(isNotEqualTo|isNotSameAs)");

  private static final Matcher<MethodInvocationTree> EQUALS_MATCHER =
      allOf(
          instanceMethod()
              .onDescendantOf("com.google.common.truth.Subject")
              .withNameMatching(EQUALS_SAME)
              .withParameters("java.lang.Object"),
          receiverSameAsParentsArgument());

  private static final Matcher<MethodInvocationTree> NOT_EQUALS_MATCHER =
      allOf(
          instanceMethod()
              .onDescendantOf("com.google.common.truth.Subject")
              .withNameMatching(NOT_EQUALS_NOT_SAME)
              .withParameters("java.lang.Object"),
          receiverSameAsParentsArgument());

  private static final Matcher<ExpressionTree> ASSERT_THAT =
      anyOf(
          staticMethod().onClass("com.google.common.truth.Truth").named("assertThat"),
          instanceMethod().onDescendantOf("com.google.common.truth.TestVerb").named("that"),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
              .named("that"));

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (methodInvocationTree.getArguments().isEmpty()) {
      return Description.NO_MATCH;
    }
    Description.Builder description = buildDescription(methodInvocationTree);
    ExpressionTree toReplace = methodInvocationTree.getArguments().get(0);
    if (EQUALS_MATCHER.matches(methodInvocationTree, state)) {
      description
          .setMessage(
              generateSummary(
                  ASTHelpers.getSymbol(methodInvocationTree).getSimpleName().toString(), "passes"))
          .addFix(suggestEqualsTesterFix(methodInvocationTree, toReplace));
    } else if (NOT_EQUALS_MATCHER.matches(methodInvocationTree, state)) {
      description.setMessage(
          generateSummary(
              ASTHelpers.getSymbol(methodInvocationTree).getSimpleName().toString(), "fails"));
    } else {
      return Description.NO_MATCH;
    }
    Fix fix = SelfEquals.fieldFix(toReplace, state);
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

  private static Matcher<? super MethodInvocationTree> receiverSameAsParentsArgument() {
    return new Matcher<MethodInvocationTree>() {
      @Override
      public boolean matches(MethodInvocationTree t, VisitorState state) {
        ExpressionTree rec = ASTHelpers.getReceiver(t);
        if (rec == null) {
          return false;
        }
        if (!ASSERT_THAT.matches(rec, state)) {
          return false;
        }
        if (!ASTHelpers.sameVariable(
            getOnlyElement(((MethodInvocationTree) rec).getArguments()),
            getOnlyElement(t.getArguments()))) {
          return false;
        }
        return true;
      }
    };
  }

  private static Fix suggestEqualsTesterFix(
      MethodInvocationTree methodInvocationTree, ExpressionTree toReplace) {
    String equalsTesterSuggest =
        "new EqualsTester().addEqualityGroup(" + toReplace + ").testEquals()";
    return SuggestedFix.builder()
        .replace(methodInvocationTree, equalsTesterSuggest)
        .addImport("com.google.common.testing.EqualsTester")
        .build();
  }
}
