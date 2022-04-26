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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.time.DurationGetTemporalUnit.getInvalidChronoUnit;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
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
 * Bans calls to {@code Duration} APIs where the {@link java.time.temporal.TemporalUnit} is not
 * {@link ChronoUnit#DAYS} or it has an estimated duration (which is guaranteed to throw an {@code
 * DateTimeException}).
 */
@BugPattern(
    summary = "Duration APIs only work for DAYS or exact durations.",
    explanation =
        "Duration APIs only work for TemporalUnits with exact durations or"
            + " ChronoUnit.DAYS. E.g., Duration.of(1, ChronoUnit.YEARS) is guaranteed to throw a"
            + " DateTimeException.",
    severity = ERROR)
public final class DurationTemporalUnit extends BugChecker implements MethodInvocationTreeMatcher {

  private static final String DURATION = "java.time.Duration";
  private static final String TEMPORAL_UNIT = "java.time.temporal.TemporalUnit";

  private static final Matcher<ExpressionTree> DURATION_OF_LONG_TEMPORAL_UNIT =
      allOf(
          anyOf(
              staticMethod().onClass(DURATION).named("of").withParameters("long", TEMPORAL_UNIT),
              instanceMethod()
                  .onExactClass(DURATION)
                  .named("minus")
                  .withParameters("long", TEMPORAL_UNIT),
              instanceMethod()
                  .onExactClass(DURATION)
                  .named("plus")
                  .withParameters("long", TEMPORAL_UNIT)),
          Matchers.not(Matchers.packageStartsWith("java.")));

  private static final ImmutableSet<ChronoUnit> INVALID_TEMPORAL_UNITS =
      Arrays.stream(ChronoUnit.values())
          .filter(c -> c.isDurationEstimated())
          .filter(c -> !c.equals(ChronoUnit.DAYS)) // DAYS is explicitly allowed
          .collect(toImmutableEnumSet());

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (DURATION_OF_LONG_TEMPORAL_UNIT.matches(tree, state)) {
      if (getInvalidChronoUnit(tree.getArguments().get(1), INVALID_TEMPORAL_UNITS).isPresent()) {
        return describeMatch(tree);
      }
    }
    return Description.NO_MATCH;
  }
}
