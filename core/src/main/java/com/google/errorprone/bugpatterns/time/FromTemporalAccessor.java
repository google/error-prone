/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.packageStartsWith;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiverType;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Bans calls to {@code javaTimeType.from(temporalAmount)} where the call is guaranteed to either:
 *
 * <ul>
 *   <li>throw a {@code DateTimeException} at runtime (e.g., {@code LocalDate.from(month)})
 *   <li>return the same parameter (e.g., {@code Instant.from(instant)})
 * </ul>
 *
 * @author kak@google.com (Kurt Alfred Kluever)
 */
@BugPattern(
    name = "FromTemporalAccessor",
    summary =
        "Certain combinations of javaTimeType.from(TemporalAccessor) will always throw a"
            + " DateTimeException or return the parameter directly.",
    explanation =
        "Not all java.time types can be created via from(TemporalAccessor). For example, you can"
            + " create a Month from a LocalDate (Month.from(localDate)) because a LocalDate"
            + " consists of a year, month, and day. However, you cannot create a LocalDate from a"
            + " Month (since it doesn't have the year or day information). Instead of throwing a"
            + " DateTimeException at runtime, this checker validates the type transformations at"
            + " compile time using static type information.",
    severity = ERROR)
public final class FromTemporalAccessor extends BugChecker implements MethodInvocationTreeMatcher {

  private static final String TEMPORAL_ACCESSOR = "java.time.temporal.TemporalAccessor";

  private static final Matcher<ExpressionTree> FROM_MATCHER =
      staticMethod().anyClass().named("from").withParameters(TEMPORAL_ACCESSOR);

  private static final Matcher<ExpressionTree> PACKAGE_MATCHER =
      anyOf(
          packageStartsWith("java.time"),
          packageStartsWith("org.threeten.extra"),
          packageStartsWith("tck.java.time"));

  private static final ImmutableMap<Matcher<Tree>, Matcher<ExpressionTree>> BAD_VALUE_FROM_KEY =
      new ImmutableMap.Builder<Matcher<Tree>, Matcher<ExpressionTree>>()
          .put(
              isSameType(DayOfWeek.class),
              makeValue(
                  Instant.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  LocalTime.class.getName(),
                  Month.class.getName(),
                  MonthDay.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  Year.class.getName(),
                  YearMonth.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName(),
                  "org.threeten.extra.AmPm",
                  "org.threeten.extra.DayOfMonth",
                  "org.threeten.extra.DayOfYear",
                  "org.threeten.extra.Quarter",
                  "org.threeten.extra.YearQuarter",
                  "org.threeten.extra.YearWeek"))
          .put(
              isSameType(Instant.class),
              makeValue(
                  DayOfWeek.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  LocalTime.class.getName(),
                  Month.class.getName(),
                  MonthDay.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  Year.class.getName(),
                  YearMonth.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName(),
                  "org.threeten.extra.AmPm",
                  "org.threeten.extra.DayOfMonth",
                  "org.threeten.extra.DayOfYear",
                  "org.threeten.extra.Quarter",
                  "org.threeten.extra.YearQuarter",
                  "org.threeten.extra.YearWeek"))
          .put(
              isSameType(LocalDate.class),
              makeValue(
                  Instant.class.getName(),
                  LocalDateTime.class.getName(),
                  LocalTime.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName(),
                  "org.threeten.extra.AmPm"))
          .put(
              isSameType(LocalDateTime.class),
              makeValue(
                  Instant.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName()))
          .put(
              isSameType(LocalTime.class),
              makeValue(
                  DayOfWeek.class.getName(),
                  Instant.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  Month.class.getName(),
                  MonthDay.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  Year.class.getName(),
                  YearMonth.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName(),
                  "org.threeten.extra.DayOfMonth",
                  "org.threeten.extra.DayOfYear",
                  "org.threeten.extra.Quarter",
                  "org.threeten.extra.YearQuarter",
                  "org.threeten.extra.YearWeek"))
          .put(
              isSameType(Month.class),
              makeValue(
                  DayOfWeek.class.getName(),
                  Instant.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  LocalTime.class.getName(),
                  MonthDay.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  Year.class.getName(),
                  YearMonth.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName(),
                  "org.threeten.extra.AmPm",
                  "org.threeten.extra.DayOfMonth",
                  "org.threeten.extra.DayOfYear",
                  "org.threeten.extra.YearQuarter",
                  "org.threeten.extra.YearWeek"))
          .put(
              isSameType(MonthDay.class),
              makeValue(
                  DayOfWeek.class.getName(),
                  Instant.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  LocalTime.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  Year.class.getName(),
                  YearMonth.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName(),
                  "org.threeten.extra.AmPm",
                  "org.threeten.extra.DayOfYear",
                  "org.threeten.extra.YearQuarter",
                  "org.threeten.extra.YearWeek"))
          .put(isSameType(OffsetDateTime.class), makeValue())
          .put(
              isSameType(OffsetTime.class),
              makeValue(
                  DayOfWeek.class.getName(),
                  Instant.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  Month.class.getName(),
                  MonthDay.class.getName(),
                  OffsetDateTime.class.getName(),
                  Year.class.getName(),
                  YearMonth.class.getName(),
                  ZonedDateTime.class.getName(),
                  "org.threeten.extra.DayOfMonth",
                  "org.threeten.extra.DayOfYear",
                  "org.threeten.extra.Quarter",
                  "org.threeten.extra.YearQuarter",
                  "org.threeten.extra.YearWeek"))
          .put(
              isSameType(Year.class),
              makeValue(
                  DayOfWeek.class.getName(),
                  Instant.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  LocalTime.class.getName(),
                  Month.class.getName(),
                  MonthDay.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  YearMonth.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName(),
                  "org.threeten.extra.AmPm",
                  "org.threeten.extra.DayOfMonth",
                  "org.threeten.extra.DayOfYear",
                  "org.threeten.extra.Quarter",
                  "org.threeten.extra.YearQuarter",
                  "org.threeten.extra.YearWeek"))
          .put(
              isSameType(YearMonth.class),
              makeValue(
                  DayOfWeek.class.getName(),
                  Instant.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  LocalTime.class.getName(),
                  MonthDay.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName(),
                  "org.threeten.extra.AmPm",
                  "org.threeten.extra.DayOfMonth",
                  "org.threeten.extra.DayOfYear",
                  "org.threeten.extra.YearWeek"))
          .put(isSameType(ZonedDateTime.class), makeValue())
          .put(
              isSameType(ZoneOffset.class),
              makeValue(
                  DayOfWeek.class.getName(),
                  Instant.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  LocalTime.class.getName(),
                  Month.class.getName(),
                  MonthDay.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  Year.class.getName(),
                  YearMonth.class.getName(),
                  ZonedDateTime.class.getName(),
                  "org.threeten.extra.AmPm",
                  "org.threeten.extra.DayOfMonth",
                  "org.threeten.extra.DayOfYear",
                  "org.threeten.extra.Quarter",
                  "org.threeten.extra.YearQuarter",
                  "org.threeten.extra.YearWeek"))
          .put(
              isSameType("org.threeten.extra.AmPm"),
              makeValue(
                  DayOfWeek.class.getName(),
                  Instant.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  LocalTime.class.getName(),
                  Month.class.getName(),
                  MonthDay.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  Year.class.getName(),
                  YearMonth.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName(),
                  "org.threeten.extra.DayOfMonth",
                  "org.threeten.extra.DayOfYear",
                  "org.threeten.extra.Quarter",
                  "org.threeten.extra.YearQuarter",
                  "org.threeten.extra.YearWeek"))
          .put(
              isSameType("org.threeten.extra.DayOfMonth"),
              makeValue(
                  DayOfWeek.class.getName(),
                  Instant.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  LocalTime.class.getName(),
                  Month.class.getName(),
                  MonthDay.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  Year.class.getName(),
                  YearMonth.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName(),
                  "org.threeten.extra.AmPm",
                  "org.threeten.extra.DayOfYear",
                  "org.threeten.extra.Quarter",
                  "org.threeten.extra.YearQuarter",
                  "org.threeten.extra.YearWeek"))
          .put(
              isSameType("org.threeten.extra.DayOfYear"),
              makeValue(
                  DayOfWeek.class.getName(),
                  Instant.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  LocalTime.class.getName(),
                  Month.class.getName(),
                  MonthDay.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  Year.class.getName(),
                  YearMonth.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName(),
                  "org.threeten.extra.AmPm",
                  "org.threeten.extra.DayOfMonth",
                  "org.threeten.extra.Quarter",
                  "org.threeten.extra.YearQuarter",
                  "org.threeten.extra.YearWeek"))
          .put(
              isSameType("org.threeten.extra.Quarter"),
              makeValue(
                  DayOfWeek.class.getName(),
                  Instant.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  LocalTime.class.getName(),
                  Month.class.getName(),
                  MonthDay.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  Year.class.getName(),
                  YearMonth.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName(),
                  "org.threeten.extra.AmPm",
                  "org.threeten.extra.DayOfMonth",
                  "org.threeten.extra.DayOfYear",
                  "org.threeten.extra.YearQuarter",
                  "org.threeten.extra.YearWeek"))
          .put(
              isSameType("org.threeten.extra.YearQuarter"),
              makeValue(
                  DayOfWeek.class.getName(),
                  Instant.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  LocalTime.class.getName(),
                  Month.class.getName(),
                  MonthDay.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  YearMonth.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName(),
                  "org.threeten.extra.AmPm",
                  "org.threeten.extra.DayOfMonth",
                  "org.threeten.extra.DayOfYear",
                  "org.threeten.extra.YearWeek"))
          .put(
              isSameType("org.threeten.extra.YearWeek"),
              makeValue(
                  DayOfWeek.class.getName(),
                  Instant.class.getName(),
                  LocalDate.class.getName(),
                  LocalDateTime.class.getName(),
                  LocalTime.class.getName(),
                  Month.class.getName(),
                  MonthDay.class.getName(),
                  OffsetDateTime.class.getName(),
                  OffsetTime.class.getName(),
                  Year.class.getName(),
                  YearMonth.class.getName(),
                  ZonedDateTime.class.getName(),
                  ZoneOffset.class.getName(),
                  "org.threeten.extra.AmPm",
                  "org.threeten.extra.DayOfMonth",
                  "org.threeten.extra.DayOfYear",
                  "org.threeten.extra.Quarter",
                  "org.threeten.extra.YearQuarter"))
          .build();

  private static Matcher<ExpressionTree> makeValue(String... classes) {
    return staticMethod().onClassAny(classes).named("from").withParameters(TEMPORAL_ACCESSOR);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    // exit early if we're not in a `from(TemporalAccessor)` method
    // this should be a large performance win since nearly all MITs will short-circuit here
    if (!FROM_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    // exit early if we're inside java.time or ThreeTen-Extra
    if (PACKAGE_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    // exit early if the receiver isn't a java.time or ThreeTen-Extra type
    String receiverType = getReceiverType(tree).toString();
    if (!receiverType.startsWith("java.time") && !receiverType.startsWith("org.threeten.extra")) {
      return Description.NO_MATCH;
    }

    ExpressionTree arg0 = getOnlyElement(tree.getArguments());
    Type type0 = getType(arg0);
    // exit early if the parameter is statically typed as a TemporalAccessor
    if (isSameType(type0, state.getTypeFromString(TEMPORAL_ACCESSOR), state)) {
      return Description.NO_MATCH;
    }

    // prevent `Instant.from(instant)` and similar
    if (isSameType(getType(tree), type0, state)) {
      SuggestedFix.Builder builder = SuggestedFix.builder();
      builder.replace(tree, state.getSourceForNode(arg0));
      return describeMatch(tree, builder.build());
    }

    for (Map.Entry<Matcher<Tree>, Matcher<ExpressionTree>> entry : BAD_VALUE_FROM_KEY.entrySet()) {
      Matcher<ExpressionTree> fromMatcher = entry.getValue(); // matches Type.from(TemporalAccessor)
      Matcher<Tree> argumentMatcher = entry.getKey(); // ensures the arg0 is a "bad type"
      if (fromMatcher.matches(tree, state) && argumentMatcher.matches(arg0, state)) {
        return describeMatch(tree);
      }
    }
    return Description.NO_MATCH;
  }
}
