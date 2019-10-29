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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.packageStartsWith;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_DAY;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_DAY;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;
import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
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

  private static Map.Entry<Matcher<ExpressionTree>, ImmutableList<ChronoField>> staticEntry(
      String className,
      String methodName,
      List<String> parameterTypes,
      List<ChronoField> parameterUnits) {
    return makeEntry(true, className, methodName, parameterTypes, parameterUnits);
  }

  private static Map.Entry<Matcher<ExpressionTree>, ImmutableList<ChronoField>> instanceEntry(
      String className,
      String methodName,
      List<String> parameterTypes,
      List<ChronoField> parameterUnits) {
    return makeEntry(false, className, methodName, parameterTypes, parameterUnits);
  }

  private static Map.Entry<Matcher<ExpressionTree>, ImmutableList<ChronoField>> makeEntry(
      boolean isStatic,
      String className,
      String methodName,
      List<String> parameterTypes,
      List<ChronoField> parameterUnits) {
    checkArgument(
        parameterTypes.size() == parameterUnits.size(),
        "There must be an equal number of parameter types (%s) and parameter units (%s):\n%s\n%s",
        parameterTypes.size(),
        parameterUnits.size(),
        parameterTypes,
        parameterUnits);
    String[] parameters = parameterTypes.toArray(new String[0]);
    Matcher<ExpressionTree> matcher =
        isStatic
            ? staticMethod().onClass(className).named(methodName).withParameters(parameters)
            : instanceMethod().onExactClass(className).named(methodName).withParameters(parameters);
    return Maps.immutableEntry(matcher, ImmutableList.copyOf(parameterUnits));
  }

  private static final ImmutableList<Map.Entry<Matcher<ExpressionTree>, ImmutableList<ChronoField>>>
      DAY_OF_WEEK_APIS =
          ImmutableList.of(
              staticEntry("java.time.DayOfWeek", "of", asList("int"), asList(DAY_OF_WEEK)));

  private static final ImmutableList<Map.Entry<Matcher<ExpressionTree>, ImmutableList<ChronoField>>>
      MONTH_APIS =
          ImmutableList.of(
              staticEntry("java.time.Month", "of", asList("int"), asList(MONTH_OF_YEAR)));

  private static final ImmutableList<Map.Entry<Matcher<ExpressionTree>, ImmutableList<ChronoField>>>
      YEAR_APIS =
          ImmutableList.of(
              staticEntry("java.time.Year", "of", asList("int"), asList(YEAR)),
              instanceEntry("java.time.Year", "atDay", asList("int"), asList(DAY_OF_YEAR)),
              instanceEntry("java.time.Year", "atMonth", asList("int"), asList(MONTH_OF_YEAR)));

  private static final String YEAR_MONTH = "java.time.YearMonth";
  private static final ImmutableList<Map.Entry<Matcher<ExpressionTree>, ImmutableList<ChronoField>>>
      YEAR_MONTH_APIS =
          ImmutableList.of(
              staticEntry(YEAR_MONTH, "of", asList("int", "int"), asList(YEAR, MONTH_OF_YEAR)),
              staticEntry(
                  YEAR_MONTH, "of", asList("int", "java.time.Month"), asList(YEAR, MONTH_OF_YEAR)),
              instanceEntry(YEAR_MONTH, "atDay", asList("int"), asList(DAY_OF_MONTH)),
              instanceEntry(YEAR_MONTH, "withMonth", asList("int"), asList(MONTH_OF_YEAR)),
              instanceEntry(YEAR_MONTH, "withYear", asList("int"), asList(YEAR)));

  private static final String MONTH_DAY = "java.time.MonthDay";
  private static final ImmutableList<Map.Entry<Matcher<ExpressionTree>, ImmutableList<ChronoField>>>
      MONTH_DAY_APIS =
          ImmutableList.of(
              staticEntry(
                  MONTH_DAY, "of", asList("int", "int"), asList(MONTH_OF_YEAR, DAY_OF_MONTH)),
              staticEntry(
                  MONTH_DAY,
                  "of",
                  asList("java.time.Month", "int"),
                  asList(MONTH_OF_YEAR, DAY_OF_MONTH)),
              instanceEntry(MONTH_DAY, "atYear", asList("int"), asList(YEAR)),
              instanceEntry(MONTH_DAY, "withDayOfMonth", asList("int"), asList(DAY_OF_MONTH)),
              instanceEntry(MONTH_DAY, "withMonth", asList("int"), asList(MONTH_OF_YEAR)));

  private static final String LOCAL_TIME = "java.time.LocalTime";
  private static final ImmutableList<Map.Entry<Matcher<ExpressionTree>, ImmutableList<ChronoField>>>
      LOCAL_TIME_APIS =
          ImmutableList.of(
              staticEntry(
                  LOCAL_TIME, "of", asList("int", "int"), asList(HOUR_OF_DAY, MINUTE_OF_HOUR)),
              staticEntry(
                  LOCAL_TIME,
                  "of",
                  asList("int", "int", "int"),
                  asList(HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE)),
              staticEntry(
                  LOCAL_TIME,
                  "of",
                  asList("int", "int", "int", "int"),
                  asList(HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE, NANO_OF_SECOND)),
              staticEntry(LOCAL_TIME, "ofNanoOfDay", asList("long"), asList(NANO_OF_DAY)),
              staticEntry(LOCAL_TIME, "ofSecondOfDay", asList("long"), asList(SECOND_OF_DAY)),
              instanceEntry(LOCAL_TIME, "withHour", asList("int"), asList(HOUR_OF_DAY)),
              instanceEntry(LOCAL_TIME, "withMinute", asList("int"), asList(MINUTE_OF_HOUR)),
              instanceEntry(LOCAL_TIME, "withNano", asList("int"), asList(NANO_OF_SECOND)),
              instanceEntry(LOCAL_TIME, "withSecond", asList("int"), asList(SECOND_OF_MINUTE)));

  private static final String LOCAL_DATE = "java.time.LocalDate";
  private static final ImmutableList<Map.Entry<Matcher<ExpressionTree>, ImmutableList<ChronoField>>>
      LOCAL_DATE_APIS =
          ImmutableList.of(
              staticEntry(
                  LOCAL_DATE,
                  "of",
                  asList("int", "int", "int"),
                  asList(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)),
              staticEntry(
                  LOCAL_DATE,
                  "of",
                  asList("int", "java.time.Month", "int"),
                  asList(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)),
              staticEntry(LOCAL_DATE, "ofEpochDay", asList("long"), asList(EPOCH_DAY)),
              staticEntry(LOCAL_DATE, "ofYearDay", asList("int", "int"), asList(YEAR, DAY_OF_YEAR)),
              instanceEntry(LOCAL_DATE, "withDayOfMonth", asList("int"), asList(DAY_OF_MONTH)),
              instanceEntry(LOCAL_DATE, "withDayOfYear", asList("int"), asList(DAY_OF_YEAR)),
              instanceEntry(LOCAL_DATE, "withMonth", asList("int"), asList(MONTH_OF_YEAR)),
              instanceEntry(LOCAL_DATE, "withYear", asList("int"), asList(YEAR)));

  private static final String LOCAL_DATE_TIME = "java.time.LocalDateTime";
  private static final ImmutableList<Map.Entry<Matcher<ExpressionTree>, ImmutableList<ChronoField>>>
      LOCAL_DATE_TIME_APIS =
          ImmutableList.of(
              staticEntry(
                  LOCAL_DATE_TIME,
                  "of",
                  asList("int", "int", "int", "int", "int"),
                  asList(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR)),
              staticEntry(
                  LOCAL_DATE_TIME,
                  "of",
                  asList("int", "java.time.Month", "int", "int", "int"),
                  asList(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR)),
              staticEntry(
                  LOCAL_DATE_TIME,
                  "of",
                  asList("int", "int", "int", "int", "int", "int"),
                  asList(
                      YEAR,
                      MONTH_OF_YEAR,
                      DAY_OF_MONTH,
                      HOUR_OF_DAY,
                      MINUTE_OF_HOUR,
                      SECOND_OF_MINUTE)),
              staticEntry(
                  LOCAL_DATE_TIME,
                  "of",
                  asList("int", "java.time.Month", "int", "int", "int", "int"),
                  asList(
                      YEAR,
                      MONTH_OF_YEAR,
                      DAY_OF_MONTH,
                      HOUR_OF_DAY,
                      MINUTE_OF_HOUR,
                      SECOND_OF_MINUTE)),
              staticEntry(
                  LOCAL_DATE_TIME,
                  "of",
                  asList("int", "int", "int", "int", "int", "int", "int"),
                  asList(
                      YEAR,
                      MONTH_OF_YEAR,
                      DAY_OF_MONTH,
                      HOUR_OF_DAY,
                      MINUTE_OF_HOUR,
                      SECOND_OF_MINUTE,
                      NANO_OF_SECOND)),
              staticEntry(
                  LOCAL_DATE_TIME,
                  "of",
                  asList("int", "java.time.Month", "int", "int", "int", "int", "int"),
                  asList(
                      YEAR,
                      MONTH_OF_YEAR,
                      DAY_OF_MONTH,
                      HOUR_OF_DAY,
                      MINUTE_OF_HOUR,
                      SECOND_OF_MINUTE,
                      NANO_OF_SECOND)),
              instanceEntry(LOCAL_DATE_TIME, "withDayOfMonth", asList("int"), asList(DAY_OF_MONTH)),
              instanceEntry(LOCAL_DATE_TIME, "withDayOfYear", asList("int"), asList(DAY_OF_YEAR)),
              instanceEntry(LOCAL_DATE_TIME, "withHour", asList("int"), asList(HOUR_OF_DAY)),
              instanceEntry(LOCAL_DATE_TIME, "withMinute", asList("int"), asList(MINUTE_OF_HOUR)),
              instanceEntry(LOCAL_DATE_TIME, "withMonth", asList("int"), asList(MONTH_OF_YEAR)),
              instanceEntry(LOCAL_DATE_TIME, "withNano", asList("int"), asList(NANO_OF_SECOND)),
              instanceEntry(LOCAL_DATE_TIME, "withSecond", asList("int"), asList(SECOND_OF_MINUTE)),
              instanceEntry(LOCAL_DATE_TIME, "withYear", asList("int"), asList(YEAR)));

  private static final ImmutableMap<Matcher<ExpressionTree>, ImmutableList<ChronoField>> APIS =
      ImmutableMap.<Matcher<ExpressionTree>, ImmutableList<ChronoField>>builder()
          // put the more popular types at the beginning for a slight speed-up
          .putAll(LOCAL_TIME_APIS)
          .putAll(LOCAL_DATE_APIS)
          .putAll(LOCAL_DATE_TIME_APIS)
          .putAll(DAY_OF_WEEK_APIS)
          .putAll(MONTH_APIS)
          .putAll(MONTH_DAY_APIS)
          .putAll(YEAR_APIS)
          .putAll(YEAR_MONTH_APIS)
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
        for (int i = 0; i < arguments.size(); i++) {
          ExpressionTree argument = arguments.get(i);
          Number constant = ASTHelpers.constValue(argument, Number.class);
          if (constant != null) {
            try {
              entry.getValue().get(i).checkValidValue(constant.longValue());
            } catch (DateTimeException invalid) {
              return buildDescription(argument).setMessage(invalid.getMessage()).build();
            }
          }
        }
        // we short-circuit the loop here; only 1 matcher will ever match, so there's no sense in
        // checking the rest of them
        return Description.NO_MATCH;
      }
    }
    return Description.NO_MATCH;
  }
}
