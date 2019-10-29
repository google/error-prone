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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.packageStartsWith;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_DAY;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_DAY;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;

/**
 * This checker errors on calls to {@code java.time} methods using values that are guaranteed to
 * throw a {@link DateTimeException}.
 *
 * @author kak@google.com (Kurt Alfred Kluever)
 */
@BugPattern(
    name = "InvalidJavaTimeConstant",
    summary =
        "This checker errors on calls to java.time methods using literals that are guaranteed to "
            + "throw a DateTimeException.",
    severity = ERROR)
public final class InvalidJavaTimeConstant extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final String LOCAL_TIME = "java.time.LocalTime";
  private static final String LOCAL_DATE = "java.time.LocalDate";
  private static final String LOCAL_DATE_TIME = "java.time.LocalDateTime";

  // TODO(kak): We could consider making a helper method for creating the mappings; e.g.,
  // private static Entry<Matcher<ExpressionTree>, ImmutableList<ChronoField>> makeEntry(
  //     boolean isStatic, String className, String methodName,
  //     Map<String, ChronoField> paramsAndUnits) { ... }
  // TODO(kak): Consider making this an ImmutableListMultimap instead
  private static final ImmutableMap<Matcher<ExpressionTree>, ImmutableList<ChronoField>> APIS =
      ImmutableMap.<Matcher<ExpressionTree>, ImmutableList<ChronoField>>builder()
          // java.time.DayOfWeek
          .put(
              staticMethod().onClass("java.time.DayOfWeek").named("of").withParameters("int"),
              ImmutableList.of(DAY_OF_WEEK))
          // java.time.Month
          .put(
              staticMethod().onClass("java.time.Month").named("of").withParameters("int"),
              ImmutableList.of(MONTH_OF_YEAR))
          // java.time.MonthDay
          .put(
              staticMethod().onClass("java.time.MonthDay").named("of").withParameters("int", "int"),
              ImmutableList.of(MONTH_OF_YEAR, DAY_OF_MONTH))
          .put(
              staticMethod()
                  .onClass("java.time.MonthDay")
                  .named("of")
                  .withParameters("java.time.Month", "int"),
              ImmutableList.of(MONTH_OF_YEAR, DAY_OF_MONTH))
          // java.time.Year
          .put(
              staticMethod().onClass("java.time.Year").named("of").withParameters("int"),
              ImmutableList.of(YEAR))
          // java.time.YearMonth
          .put(
              staticMethod()
                  .onClass("java.time.YearMonth")
                  .named("of")
                  .withParameters("int", "int"),
              ImmutableList.of(YEAR, MONTH_OF_YEAR))
          .put(
              staticMethod()
                  .onClass("java.time.YearMonth")
                  .named("of")
                  .withParameters("int", "java.time.Month"),
              ImmutableList.of(YEAR, MONTH_OF_YEAR))
          // java.time.LocalTime
          .put(
              staticMethod().onClass(LOCAL_TIME).named("of").withParameters("int", "int"),
              ImmutableList.of(HOUR_OF_DAY, MINUTE_OF_HOUR))
          .put(
              staticMethod().onClass(LOCAL_TIME).named("of").withParameters("int", "int", "int"),
              ImmutableList.of(HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE))
          .put(
              staticMethod()
                  .onClass(LOCAL_TIME)
                  .named("of")
                  .withParameters("int", "int", "int", "int"),
              ImmutableList.of(HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE, NANO_OF_SECOND))
          .put(
              staticMethod().onClass(LOCAL_TIME).named("ofNanoOfDay").withParameters("long"),
              ImmutableList.of(NANO_OF_DAY))
          .put(
              staticMethod().onClass(LOCAL_TIME).named("ofSecondOfDay").withParameters("long"),
              ImmutableList.of(SECOND_OF_DAY))
          // java.time.LocalDate
          .put(
              staticMethod().onClass(LOCAL_DATE).named("of").withParameters("int", "int", "int"),
              ImmutableList.of(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH))
          .put(
              staticMethod()
                  .onClass(LOCAL_DATE)
                  .named("of")
                  .withParameters("int", "java.time.Month", "int"),
              ImmutableList.of(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH))
          // NOTE: do not try to add LocalDate.ofEpochDay(long), EPOCH_DAY; it's incorrect
          .put(
              staticMethod().onClass(LOCAL_DATE).named("ofYearDay").withParameters("int", "int"),
              ImmutableList.of(YEAR, DAY_OF_YEAR))
          // java.time.LocalDateTime
          .put(
              staticMethod()
                  .onClass(LOCAL_DATE_TIME)
                  .named("of")
                  .withParameters("int", "int", "int", "int", "int"),
              ImmutableList.of(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR))
          .put(
              staticMethod()
                  .onClass(LOCAL_DATE_TIME)
                  .named("of")
                  .withParameters("int", "java.time.Month", "int", "int", "int"),
              ImmutableList.of(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR))
          .put(
              staticMethod()
                  .onClass(LOCAL_DATE_TIME)
                  .named("of")
                  .withParameters("int", "int", "int", "int", "int", "int"),
              ImmutableList.of(
                  YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE))
          .put(
              staticMethod()
                  .onClass(LOCAL_DATE_TIME)
                  .named("of")
                  .withParameters("int", "java.time.Month", "int", "int", "int", "int"),
              ImmutableList.of(
                  YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE))
          .put(
              staticMethod()
                  .onClass(LOCAL_DATE_TIME)
                  .named("of")
                  .withParameters("int", "int", "int", "int", "int", "int", "int"),
              ImmutableList.of(
                  YEAR,
                  MONTH_OF_YEAR,
                  DAY_OF_MONTH,
                  HOUR_OF_DAY,
                  MINUTE_OF_HOUR,
                  SECOND_OF_MINUTE,
                  NANO_OF_SECOND))
          .put(
              staticMethod()
                  .onClass(LOCAL_DATE_TIME)
                  .named("of")
                  .withParameters("int", "java.time.Month", "int", "int", "int", "int", "int"),
              ImmutableList.of(
                  YEAR,
                  MONTH_OF_YEAR,
                  DAY_OF_MONTH,
                  HOUR_OF_DAY,
                  MINUTE_OF_HOUR,
                  SECOND_OF_MINUTE,
                  NANO_OF_SECOND))
          // TODO(kak): Add instance methods to this as well
          .build();

  private static final Matcher<ExpressionTree> JAVA_MATCHER =
      anyOf(packageStartsWith("java."), packageStartsWith("tck.java."));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    // Allow the JDK to do whatever it wants (unit tests, etc.)
    if (JAVA_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    for (Map.Entry<Matcher<ExpressionTree>, ImmutableList<ChronoField>> entry : APIS.entrySet()) {
      if (entry.getKey().matches(tree, state)) {
        List<? extends ExpressionTree> arguments = tree.getArguments();
        ImmutableList<ChronoField> units = entry.getValue();

        // if units.size() != arguments.size() then we screwed something up in the static map of
        // matchers above (there should always be an equal number of arguments and units)
        if (units.size() == arguments.size()) {
          for (int i = 0; i < arguments.size(); i++) {
            ExpressionTree argument = arguments.get(i);
            Number constant = ASTHelpers.constValue(argument, Number.class);
            if (constant != null) {
              try {
                units.get(i).checkValidValue(constant.longValue());
              } catch (DateTimeException invalid) {
                return buildDescription(argument).setMessage(invalid.getMessage()).build();
              }
            }
          }
        }
      }
    }
    return Description.NO_MATCH;
  }
}
