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
package com.google.errorprone.bugpatterns.time;

import static com.google.common.collect.Sets.toImmutableEnumSet;
import static com.google.errorprone.BugPattern.ProvidesFix.NO_FIX;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.time.DurationGetTemporalUnit.getInvalidChronoUnit;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.collect.ImmutableSet;
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
import java.util.Arrays;

/**
 * Bans calls to {@code Duration.of(long, TemporalUnit)} where the {@link
 * java.time.temporal.TemporalUnit} has an estimated duration (which is guaranteed to throw an
 * {@code DateTimeException}).
 */
@BugPattern(
    name = "DurationOfLongTemporalUnit",
    summary = "Duration.of(long, TemporalUnit) only works for TemporalUnits with exact durations.",
    explanation =
        "Duration.of(long, TemporalUnit) only works for TemporalUnits with exact durations. "
            + "E.g., Duration.of(1, ChronoUnit.YEAR) is guaranteed to throw a DateTimeException.",
    severity = ERROR,
    providesFix = NO_FIX)
public final class DurationOfLongTemporalUnit extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> DURATION_OF_LONG_TEMPORAL_UNIT =
      allOf(
          staticMethod()
              .onClass("java.time.Duration")
              .named("of")
              .withParameters("long", "java.time.temporal.TemporalUnit"),
          Matchers.not(Matchers.packageStartsWith("java.")));

  private static final ImmutableSet<ChronoUnit> INVALID_TEMPORAL_UNITS =
      Arrays.stream(ChronoUnit.values())
          .filter(c -> c.isDurationEstimated())
          .collect(toImmutableEnumSet());

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (DURATION_OF_LONG_TEMPORAL_UNIT.matches(tree, state)) {
      if (getInvalidChronoUnit(tree.getArguments().get(1), INVALID_TEMPORAL_UNITS).isPresent()) {
        // TODO(kak): do we want to include the name of the invalid ChronoUnit in the message?
        return describeMatch(tree);
      }
    }
    return Description.NO_MATCH;
  }
}
