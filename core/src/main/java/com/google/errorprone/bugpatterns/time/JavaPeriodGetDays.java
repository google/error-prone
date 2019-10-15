/*
 * Copyright 2018 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns.time;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.time.NearbyCallers.containsCallToSameReceiverNearby;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * This checker warns about calls to {@code period.getDays()} without a corresponding "nearby" call
 * to {@code period.getYears(), period.getMonths(), or period.getTotalMonths()}.
 *
 * @author glorioso@google.com (Nick Glorioso)
 */
@BugPattern(
    name = "JavaPeriodGetDays",
    summary =
        "period.getDays() only accesses the \"days\" portion of the Period, and doesn't represent"
            + " the total span of time of the period. Consider using org.threeten.extra.Days to"
            + " extract the difference between two civil dates if you want the whole time.",
    severity = WARNING)
public final class JavaPeriodGetDays extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> PERIOD_LOOK_AT_OTHERS =
      instanceMethod()
          .onExactClass("java.time.Period")
          .namedAnyOf("getMonths", "getYears", "getTotalMonths");
  private static final Matcher<ExpressionTree> PERIOD_GET_DAYS =
      allOf(
          instanceMethod().onExactClass("java.time.Period").named("getDays"),
          Matchers.not(Matchers.packageStartsWith("java.")));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (PERIOD_GET_DAYS.matches(tree, state)) {
      if (!containsCallToSameReceiverNearby(
          tree, PERIOD_LOOK_AT_OTHERS, state, /*checkProtoChains=*/ false)) {
        return describeMatch(tree);
      }
    }
    return Description.NO_MATCH;
  }
}
