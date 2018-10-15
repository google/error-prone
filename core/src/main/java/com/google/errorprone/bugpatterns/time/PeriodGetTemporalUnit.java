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

import static com.google.errorprone.BugPattern.ProvidesFix.NO_FIX;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.time.DurationGetTemporalUnit.getInvalidChronoUnit;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

/**
 * Bans calls to {@code Period.get(temporalUnit)} where {@code temporalUnit} is not {@code YEARS},
 * {@code MONTHS}, or {@code DAYS}.
 */
@BugPattern(
    name = "PeriodGetTemporalUnit",
    summary = "Period.get() only works with YEARS, MONTHS, or DAYS.",
    explanation =
        "`Period.get(TemporalUnit)` only works when passed `ChronoUnit.YEARS`, `ChronoUnit.MONTHS`,"
            + " or `ChronoUnit.DAYS`. All other values are guaranteed to throw an"
            + " `UnsupportedTemporalTypeException`.",
    severity = ERROR,
    providesFix = NO_FIX)
public final class PeriodGetTemporalUnit extends BugChecker implements MethodInvocationTreeMatcher {
  private static final EnumSet<ChronoUnit> INVALID_TEMPORAL_UNITS =
      EnumSet.complementOf(EnumSet.of(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS));

  private static final Matcher<ExpressionTree> MATCHER =
      Matchers.instanceMethod()
          .onExactClass("java.time.Period")
          .named("get")
          .withParameters("java.time.temporal.TemporalUnit");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return MATCHER.matches(tree, state)
            && getInvalidChronoUnit(tree, INVALID_TEMPORAL_UNITS).isPresent()
        ? describeMatch(tree)
        : Description.NO_MATCH;
  }
}
