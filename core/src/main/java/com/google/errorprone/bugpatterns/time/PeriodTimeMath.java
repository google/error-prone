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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/** Bans calls to {@code Period#plus/minus(TemporalAmount)} where the argument is a Duration. */
@BugPattern(
    name = "PeriodTimeMath",
    summary = "When adding or subtracting from a Period, Duration is incompatible.",
    explanation =
        "Period.(plus|minus)(TemporalAmount) will always throw a DateTimeException when passed a "
            + "Duration.",
    severity = ERROR,
    providesFix = ProvidesFix.NO_FIX)
public final class PeriodTimeMath extends BugChecker implements MethodInvocationTreeMatcher {

  private final Matcher<MethodInvocationTree> matcherToCheck;

  private static final Matcher<ExpressionTree> PERIOD_MATH =
      instanceMethod().onExactClass("java.time.Period").namedAnyOf("plus", "minus");

  private static final Matcher<ExpressionTree> DURATION = isSameType("java.time.Duration");
  private static final Matcher<ExpressionTree> PERIOD = isSameType("java.time.Period");

  public PeriodTimeMath(ErrorProneFlags flags) {
    boolean requireStrictCompatibility =
        flags.getBoolean("PeriodTimeMath:RequireStaticPeriodArgument").orElse(false);
    matcherToCheck =
        allOf(PERIOD_MATH, argument(0, requireStrictCompatibility ? not(PERIOD) : DURATION));
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return matcherToCheck.matches(tree, state) ? describeMatch(tree) : Description.NO_MATCH;
  }
}

