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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Optional;

/**
 * Bans calls to {@code Duration.get(temporalUnit)} where {@code temporalUnit} is not {@code
 * SECONDS} or {@code NANOS}.
 */
@BugPattern(
    name = "DurationGetTemporalUnit",
    summary = "Duration.get() only works with SECONDS or NANOS.",
    explanation =
        "`Duration.get(TemporalUnit)` only works when passed `ChronoUnit.SECONDS` or "
            + "`ChronoUnit.NANOS`. All other values are guaranteed to throw a "
            + "`UnsupportedTemporalTypeException`. In general, you should avoid "
            + "`duration.get(ChronoUnit)`. Instead, please use `duration.toNanos()`, "
            + "`Durations.toMicros(duration)`, `duration.toMillis()`, `duration.getSeconds()`, "
            + "`duration.toMinutes()`, `duration.toHours()`, or `duration.toDays()`.",
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class DurationGetTemporalUnit extends BugChecker
    implements MethodInvocationTreeMatcher {
  private static final EnumSet<ChronoUnit> INVALID_TEMPORAL_UNITS =
      EnumSet.complementOf(EnumSet.of(ChronoUnit.SECONDS, ChronoUnit.NANOS));

  private static final ImmutableMap<ChronoUnit, String> SUGGESTIONS =
      ImmutableMap.<ChronoUnit, String>builder()
          .put(ChronoUnit.DAYS, ".toDays()")
          .put(ChronoUnit.HOURS, ".toHours()")
          .put(ChronoUnit.MINUTES, ".toMinutes()")
          // SECONDS is omitted because it's a valid parameter
          .put(ChronoUnit.MILLIS, ".toMillis()")
          // TODO(kak): Do we want to add MICROS here? We'd need to add a static import for
          // com.google.common.time.Durations.toMicros;
          // NANOS is omitted because it's a valid parameter
          .build();

  private static final Matcher<ExpressionTree> MATCHER =
      Matchers.instanceMethod()
          .onExactClass("java.time.Duration")
          .named("get")
          .withParameters("java.time.temporal.TemporalUnit");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (MATCHER.matches(tree, state)) {
      Optional<ChronoUnit> invalidUnit = getInvalidChronoUnit(tree, INVALID_TEMPORAL_UNITS);
      if (invalidUnit.isPresent()) {
        if (SUGGESTIONS.containsKey(invalidUnit.get())) {
          SuggestedFix.Builder builder = SuggestedFix.builder();
          builder.replace(
              tree,
              state.getSourceForNode(ASTHelpers.getReceiver(tree))
                  + SUGGESTIONS.get(invalidUnit.get()));
          return describeMatch(tree, builder.build());
        }
        return describeMatch(tree);
      }
    }
    return Description.NO_MATCH;
  }

  // used by PeriodGetTemporalUnit
  static Optional<ChronoUnit> getInvalidChronoUnit(
      MethodInvocationTree tree, EnumSet<ChronoUnit> invalidUnits) {
    Optional<String> constant = getEnumName(Iterables.getOnlyElement(tree.getArguments()));
    if (constant.isPresent()) {
      for (ChronoUnit invalidTemporalUnit : invalidUnits) {
        if (constant.get().equals(invalidTemporalUnit.name())) {
          return Optional.of(invalidTemporalUnit);
        }
      }
    }
    return Optional.empty();
  }

  private static Optional<String> getEnumName(ExpressionTree temporalUnit) {
    if (temporalUnit instanceof IdentifierTree) { // e.g., SECONDS
      return Optional.of(((IdentifierTree) temporalUnit).getName().toString());
    }
    if (temporalUnit instanceof MemberSelectTree) { // e.g., ChronoUnit.SECONDS
      return Optional.of(((MemberSelectTree) temporalUnit).getIdentifier().toString());
    }
    return Optional.empty();
  }
}
