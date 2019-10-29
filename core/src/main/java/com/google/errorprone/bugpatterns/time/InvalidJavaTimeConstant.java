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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
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

  private static MatcherWithUnits createStatic(
      String className,
      String methodName,
      List<String> parameterTypes,
      List<ChronoField> parameterUnits) {
    checkEqualSizes(parameterTypes, parameterUnits);
    return new AutoValue_InvalidJavaTimeConstant_MatcherWithUnits(
        staticMethod()
            .onClass(className)
            .named(methodName)
            .withParameters(parameterTypes.toArray(new String[0])),
        ImmutableList.copyOf(parameterUnits));
  }

  private static MatcherWithUnits createInstance(
      String className,
      String methodName,
      List<String> parameterTypes,
      List<ChronoField> parameterUnits) {
    checkEqualSizes(parameterTypes, parameterUnits);
    return new AutoValue_InvalidJavaTimeConstant_MatcherWithUnits(
        instanceMethod()
            .onExactClass(className)
            .named(methodName)
            .withParameters(parameterTypes.toArray(new String[0])),
        ImmutableList.copyOf(parameterUnits));
  }

  private static void checkEqualSizes(
      List<String> parameterTypes, List<ChronoField> parameterUnits) {
    checkArgument(
        parameterTypes.size() == parameterUnits.size(),
        "There must be an equal number of parameter types (%s) and parameter units (%s):\n%s\n%s",
        parameterTypes.size(),
        parameterUnits.size(),
        parameterTypes,
        parameterUnits);
  }

  @AutoValue
  abstract static class MatcherWithUnits {
    abstract Matcher<ExpressionTree> matcher();

    abstract ImmutableList<ChronoField> units();
  }

  // Note, we use asList(...) below instead of ImmutableList.of(...) to reduce line wrapping.

  private static final ImmutableList<MatcherWithUnits> DAY_OF_WEEK_APIS =
      ImmutableList.of(
          createStatic("java.time.DayOfWeek", "of", asList("int"), asList(DAY_OF_WEEK)));

  private static final ImmutableList<MatcherWithUnits> MONTH_APIS =
      ImmutableList.of(createStatic("java.time.Month", "of", asList("int"), asList(MONTH_OF_YEAR)));

  private static final ImmutableList<MatcherWithUnits> YEAR_APIS =
      ImmutableList.of(
          createStatic("java.time.Year", "of", asList("int"), asList(YEAR)),
          createInstance("java.time.Year", "atDay", asList("int"), asList(DAY_OF_YEAR)),
          createInstance("java.time.Year", "atMonth", asList("int"), asList(MONTH_OF_YEAR)));

  private static final String YEAR_MONTH = "java.time.YearMonth";
  private static final ImmutableList<MatcherWithUnits> YEAR_MONTH_APIS =
      ImmutableList.of(
          createStatic(YEAR_MONTH, "of", asList("int", "int"), asList(YEAR, MONTH_OF_YEAR)),
          createStatic(
              YEAR_MONTH, "of", asList("int", "java.time.Month"), asList(YEAR, MONTH_OF_YEAR)),
          createInstance(YEAR_MONTH, "atDay", asList("int"), asList(DAY_OF_MONTH)),
          createInstance(YEAR_MONTH, "withMonth", asList("int"), asList(MONTH_OF_YEAR)),
          createInstance(YEAR_MONTH, "withYear", asList("int"), asList(YEAR)));

  private static final String MONTH_DAY = "java.time.MonthDay";
  private static final ImmutableList<MatcherWithUnits> MONTH_DAY_APIS =
      ImmutableList.of(
          createStatic(MONTH_DAY, "of", asList("int", "int"), asList(MONTH_OF_YEAR, DAY_OF_MONTH)),
          createStatic(
              MONTH_DAY,
              "of",
              asList("java.time.Month", "int"),
              asList(MONTH_OF_YEAR, DAY_OF_MONTH)),
          createInstance(MONTH_DAY, "atYear", asList("int"), asList(YEAR)),
          createInstance(MONTH_DAY, "withDayOfMonth", asList("int"), asList(DAY_OF_MONTH)),
          createInstance(MONTH_DAY, "withMonth", asList("int"), asList(MONTH_OF_YEAR)));

  private static final String LOCAL_TIME = "java.time.LocalTime";
  private static final ImmutableList<MatcherWithUnits> LOCAL_TIME_APIS =
      ImmutableList.of(
          createStatic(LOCAL_TIME, "of", asList("int", "int"), asList(HOUR_OF_DAY, MINUTE_OF_HOUR)),
          createStatic(
              LOCAL_TIME,
              "of",
              asList("int", "int", "int"),
              asList(HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE)),
          createStatic(
              LOCAL_TIME,
              "of",
              asList("int", "int", "int", "int"),
              asList(HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE, NANO_OF_SECOND)),
          createStatic(LOCAL_TIME, "ofNanoOfDay", asList("long"), asList(NANO_OF_DAY)),
          createStatic(LOCAL_TIME, "ofSecondOfDay", asList("long"), asList(SECOND_OF_DAY)),
          createInstance(LOCAL_TIME, "withHour", asList("int"), asList(HOUR_OF_DAY)),
          createInstance(LOCAL_TIME, "withMinute", asList("int"), asList(MINUTE_OF_HOUR)),
          createInstance(LOCAL_TIME, "withNano", asList("int"), asList(NANO_OF_SECOND)),
          createInstance(LOCAL_TIME, "withSecond", asList("int"), asList(SECOND_OF_MINUTE)));

  private static final String LOCAL_DATE = "java.time.LocalDate";
  private static final ImmutableList<MatcherWithUnits> LOCAL_DATE_APIS =
      ImmutableList.of(
          createStatic(
              LOCAL_DATE,
              "of",
              asList("int", "int", "int"),
              asList(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)),
          createStatic(
              LOCAL_DATE,
              "of",
              asList("int", "java.time.Month", "int"),
              asList(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)),
          createStatic(LOCAL_DATE, "ofEpochDay", asList("long"), asList(EPOCH_DAY)),
          createStatic(LOCAL_DATE, "ofYearDay", asList("int", "int"), asList(YEAR, DAY_OF_YEAR)),
          createInstance(LOCAL_DATE, "withDayOfMonth", asList("int"), asList(DAY_OF_MONTH)),
          createInstance(LOCAL_DATE, "withDayOfYear", asList("int"), asList(DAY_OF_YEAR)),
          createInstance(LOCAL_DATE, "withMonth", asList("int"), asList(MONTH_OF_YEAR)),
          createInstance(LOCAL_DATE, "withYear", asList("int"), asList(YEAR)));

  private static final String LOCAL_DATE_TIME = "java.time.LocalDateTime";
  private static final ImmutableList<MatcherWithUnits> LOCAL_DATE_TIME_APIS =
      ImmutableList.of(
          createStatic(
              LOCAL_DATE_TIME,
              "of",
              asList("int", "int", "int", "int", "int"),
              asList(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR)),
          createStatic(
              LOCAL_DATE_TIME,
              "of",
              asList("int", "java.time.Month", "int", "int", "int"),
              asList(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR)),
          createStatic(
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
          createStatic(
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
          createStatic(
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
          createStatic(
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
          createInstance(LOCAL_DATE_TIME, "withDayOfMonth", asList("int"), asList(DAY_OF_MONTH)),
          createInstance(LOCAL_DATE_TIME, "withDayOfYear", asList("int"), asList(DAY_OF_YEAR)),
          createInstance(LOCAL_DATE_TIME, "withHour", asList("int"), asList(HOUR_OF_DAY)),
          createInstance(LOCAL_DATE_TIME, "withMinute", asList("int"), asList(MINUTE_OF_HOUR)),
          createInstance(LOCAL_DATE_TIME, "withMonth", asList("int"), asList(MONTH_OF_YEAR)),
          createInstance(LOCAL_DATE_TIME, "withNano", asList("int"), asList(NANO_OF_SECOND)),
          createInstance(LOCAL_DATE_TIME, "withSecond", asList("int"), asList(SECOND_OF_MINUTE)),
          createInstance(LOCAL_DATE_TIME, "withYear", asList("int"), asList(YEAR)));

  private static final ImmutableList<MatcherWithUnits> APIS =
      ImmutableList.<MatcherWithUnits>builder()
          // add the more popular types at the beginning for a slight speed-up
          .addAll(LOCAL_TIME_APIS)
          .addAll(LOCAL_DATE_APIS)
          .addAll(LOCAL_DATE_TIME_APIS)
          .addAll(DAY_OF_WEEK_APIS)
          .addAll(MONTH_APIS)
          .addAll(MONTH_DAY_APIS)
          .addAll(YEAR_APIS)
          .addAll(YEAR_MONTH_APIS)
          .build();

  private static final Matcher<ExpressionTree> JAVA_MATCHER =
      anyOf(packageStartsWith("java."), packageStartsWith("tck.java."));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    // Allow the JDK to do whatever it wants (unit tests, etc.)
    if (JAVA_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    for (MatcherWithUnits matcherWithUnits : APIS) {
      if (matcherWithUnits.matcher().matches(tree, state)) {
        List<? extends ExpressionTree> arguments = tree.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
          ExpressionTree argument = arguments.get(i);
          Number constant = ASTHelpers.constValue(argument, Number.class);
          if (constant != null) {
            try {
              matcherWithUnits.units().get(i).checkValidValue(constant.longValue());
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
