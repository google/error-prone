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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.chrono.HijrahEra;
import java.time.chrono.IsoEra;
import java.time.chrono.JapaneseDate;
import java.time.chrono.JapaneseEra;
import java.time.chrono.MinguoDate;
import java.time.chrono.MinguoEra;
import java.time.chrono.ThaiBuddhistDate;
import java.time.chrono.ThaiBuddhistEra;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

/**
 * Bans calls to {@code TemporalAccessor.get(ChronoField)} where the implementation is guaranteed to
 * throw an {@code UnsupportedTemporalTypeException}.
 */
@BugPattern(
  name = "TemporalAccessorGetChronoField",
  summary = "TemporalAccessor.get() only works for certain values of ChronoField.",
  explanation =
      "TemporalAccessor.get(ChronoField) only works for certain values of ChronoField. E.g., "
          + "DayOfWeek only supports DAY_OF_WEEK. All other values are guaranteed to throw a "
          + "UnsupportedTemporalTypeException.",
  severity = ERROR,
  providesFix = NO_FIX
)
public final class TemporalAccessorGetChronoField extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final ZoneId ARBITRARY_ZONE = ZoneId.of("America/Los_Angeles");

  private static final ImmutableList<TemporalAccessor> TEMPORAL_ACCESSOR_INSTANCES =
      ImmutableList.of(
          DayOfWeek.MONDAY,
          HijrahDate.now(ARBITRARY_ZONE),
          HijrahEra.AH,
          Instant.now(),
          IsoEra.CE,
          JapaneseDate.now(ARBITRARY_ZONE),
          JapaneseEra.SHOWA,
          LocalDate.now(ARBITRARY_ZONE),
          LocalDateTime.now(ARBITRARY_ZONE),
          LocalTime.now(ARBITRARY_ZONE),
          MinguoDate.now(ARBITRARY_ZONE),
          MinguoEra.ROC,
          Month.MAY,
          MonthDay.now(ARBITRARY_ZONE),
          OffsetDateTime.now(ARBITRARY_ZONE),
          OffsetTime.now(ARBITRARY_ZONE),
          ThaiBuddhistDate.now(ARBITRARY_ZONE),
          ThaiBuddhistEra.BE,
          Year.now(ARBITRARY_ZONE),
          YearMonth.now(ARBITRARY_ZONE),
          ZonedDateTime.now(ARBITRARY_ZONE),
          ZoneOffset.ofHours(8));

  private static final ImmutableListMultimap<Class<?>, ChronoField> UNSUPPORTED =
      buildUnsupported();

  private static ImmutableListMultimap<Class<?>, ChronoField> buildUnsupported() {
    ImmutableListMultimap.Builder<Class<?>, ChronoField> builder = ImmutableListMultimap.builder();
    for (TemporalAccessor temporalAccessor : TEMPORAL_ACCESSOR_INSTANCES) {
      for (ChronoField chronoField : ChronoField.values()) {
        if (!temporalAccessor.isSupported(chronoField)) {
          builder.put(temporalAccessor.getClass(), chronoField);
        }
      }
    }
    return builder.build();
  }

  private static final ImmutableMap<Matcher<ExpressionTree>, Class<?>> MATCHER_MAP =
      buildMatcherMap();

  private static ImmutableMap<Matcher<ExpressionTree>, Class<?>> buildMatcherMap() {
    ImmutableMap.Builder<Matcher<ExpressionTree>, Class<?>> matchers = ImmutableMap.builder();
    for (Class<?> clazz : UNSUPPORTED.keySet()) {
      matchers.put(
          Matchers.instanceMethod()
              .onExactClass(clazz.getName())
              .namedAnyOf("get", "getLong")
              .withParameters("java.time.temporal.TemporalField"),
          clazz);
    }
    return matchers.build();
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    for (ImmutableMap.Entry<Matcher<ExpressionTree>, Class<?>> entry : MATCHER_MAP.entrySet()) {
      if (entry.getKey().matches(tree, state)) {
        return isDefinitelyInvalidChronoField(tree, UNSUPPORTED.get(entry.getValue()))
            ? describeMatch(tree)
            : Description.NO_MATCH;
      }
    }
    return Description.NO_MATCH;
  }

  private static boolean isDefinitelyInvalidChronoField(
      MethodInvocationTree tree, Iterable<ChronoField> invalidChronoFields) {
    Optional<String> constant = getEnumName(Iterables.getOnlyElement(tree.getArguments()));
    if (constant.isPresent()) {
      for (ChronoField invalidChronoField : invalidChronoFields) {
        if (constant.get().equals(invalidChronoField.name())) {
          return true;
        }
      }
    }
    return false;
  }

  private static Optional<String> getEnumName(ExpressionTree chronoField) {
    if (chronoField instanceof IdentifierTree) { // e.g., SECOND_OF_DAY
      return Optional.of(((IdentifierTree) chronoField).getName().toString());
    }
    if (chronoField instanceof MemberSelectTree) { // e.g., ChronoField.SECOND_OF_DAY
      return Optional.of(((MemberSelectTree) chronoField).getIdentifier().toString());
    }
    return Optional.empty();
  }
}
