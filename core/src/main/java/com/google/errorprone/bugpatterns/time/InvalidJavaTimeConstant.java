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

import static com.google.common.collect.ImmutableList.toImmutableList;
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
import static java.util.Arrays.stream;

import com.google.auto.value.AutoValue;
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
import com.sun.tools.javac.code.Type;
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
    summary =
        "This checker errors on calls to java.time methods using values that are guaranteed to "
            + "throw a DateTimeException.",
    severity = ERROR)
public final class InvalidJavaTimeConstant extends BugChecker
    implements MethodInvocationTreeMatcher {

  @AutoValue
  abstract static class MatcherWithUnits {
    abstract Matcher<ExpressionTree> matcher();

    abstract ImmutableList<ChronoField> units();
  }

  @AutoValue
  abstract static class Param {
    abstract String type();

    abstract ChronoField unit();
  }

  private static Param intP(ChronoField unit) {
    return new AutoValue_InvalidJavaTimeConstant_Param("int", unit);
  }

  private static Param longP(ChronoField unit) {
    return new AutoValue_InvalidJavaTimeConstant_Param("long", unit);
  }

  private static Param monthP(ChronoField unit) {
    return new AutoValue_InvalidJavaTimeConstant_Param("java.time.Month", unit);
  }

  @AutoValue
  abstract static class JavaTimeType {
    abstract String className();

    abstract ImmutableList<MatcherWithUnits> methods();

    public static Builder builder() {
      return new AutoValue_InvalidJavaTimeConstant_JavaTimeType.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setClassName(String className);

      abstract String className();

      abstract ImmutableList.Builder<MatcherWithUnits> methodsBuilder();

      public Builder addStaticMethod(String methodName, Param... params) {
        methodsBuilder()
            .add(
                new AutoValue_InvalidJavaTimeConstant_MatcherWithUnits(
                    staticMethod()
                        .onClass(className())
                        .named(methodName)
                        .withParameters(getParameterTypes(params)),
                    getParameterUnits(params)));
        return this;
      }

      public Builder addInstanceMethod(String methodName, Param... params) {
        methodsBuilder()
            .add(
                new AutoValue_InvalidJavaTimeConstant_MatcherWithUnits(
                    instanceMethod()
                        .onExactClass(className())
                        .named(methodName)
                        .withParameters(getParameterTypes(params)),
                    getParameterUnits(params)));
        return this;
      }

      private static ImmutableList<ChronoField> getParameterUnits(Param... params) {
        return stream(params).map(p -> p.unit()).collect(toImmutableList());
      }

      private static ImmutableList<String> getParameterTypes(Param... params) {
        return stream(params).map(p -> p.type()).collect(toImmutableList());
      }

      public abstract JavaTimeType build();
    }
  }

  private static final JavaTimeType DAY_OF_WEEK_APIS =
      JavaTimeType.builder()
          .setClassName("java.time.DayOfWeek")
          .addStaticMethod("of", intP(DAY_OF_WEEK))
          .build();

  private static final JavaTimeType MONTH_APIS =
      JavaTimeType.builder()
          .setClassName("java.time.Month")
          .addStaticMethod("of", intP(MONTH_OF_YEAR))
          .build();

  private static final JavaTimeType YEAR_APIS =
      JavaTimeType.builder()
          .setClassName("java.time.Year")
          .addStaticMethod("of", intP(YEAR))
          .addInstanceMethod("atDay", intP(DAY_OF_YEAR))
          .addInstanceMethod("atMonth", intP(MONTH_OF_YEAR))
          .build();

  private static final JavaTimeType YEAR_MONTH_APIS =
      JavaTimeType.builder()
          .setClassName("java.time.YearMonth")
          .addStaticMethod("of", intP(YEAR), intP(MONTH_OF_YEAR))
          .addStaticMethod("of", intP(YEAR), monthP(MONTH_OF_YEAR))
          .addInstanceMethod("atDay", intP(DAY_OF_MONTH))
          .addInstanceMethod("withMonth", intP(MONTH_OF_YEAR))
          .addInstanceMethod("withYear", intP(YEAR))
          .build();

  private static final JavaTimeType MONTH_DAY_APIS =
      JavaTimeType.builder()
          .setClassName("java.time.MonthDay")
          .addStaticMethod("of", intP(MONTH_OF_YEAR), intP(DAY_OF_MONTH))
          .addStaticMethod("of", monthP(MONTH_OF_YEAR), intP(DAY_OF_MONTH))
          .addInstanceMethod("atYear", intP(YEAR))
          .addInstanceMethod("withDayOfMonth", intP(DAY_OF_MONTH))
          .addInstanceMethod("withMonth", intP(MONTH_OF_YEAR))
          .build();

  private static final JavaTimeType LOCAL_TIME_APIS =
      JavaTimeType.builder()
          .setClassName("java.time.LocalTime")
          .addStaticMethod("of", intP(HOUR_OF_DAY), intP(MINUTE_OF_HOUR))
          .addStaticMethod("of", intP(HOUR_OF_DAY), intP(MINUTE_OF_HOUR), intP(SECOND_OF_MINUTE))
          .addStaticMethod(
              "of",
              intP(HOUR_OF_DAY),
              intP(MINUTE_OF_HOUR),
              intP(SECOND_OF_MINUTE),
              intP(NANO_OF_SECOND))
          .addStaticMethod("ofNanoOfDay", longP(NANO_OF_DAY))
          .addStaticMethod("ofSecondOfDay", longP(SECOND_OF_DAY))
          .addInstanceMethod("withHour", intP(HOUR_OF_DAY))
          .addInstanceMethod("withMinute", intP(MINUTE_OF_HOUR))
          .addInstanceMethod("withNano", intP(NANO_OF_SECOND))
          .addInstanceMethod("withSecond", intP(SECOND_OF_MINUTE))
          .build();

  private static final JavaTimeType LOCAL_DATE_APIS =
      JavaTimeType.builder()
          .setClassName("java.time.LocalDate")
          .addStaticMethod("of", intP(YEAR), intP(MONTH_OF_YEAR), intP(DAY_OF_MONTH))
          .addStaticMethod("of", intP(YEAR), monthP(MONTH_OF_YEAR), intP(DAY_OF_MONTH))
          .addStaticMethod("ofEpochDay", longP(EPOCH_DAY))
          .addStaticMethod("ofYearDay", intP(YEAR), intP(DAY_OF_YEAR))
          .addInstanceMethod("withDayOfMonth", intP(DAY_OF_MONTH))
          .addInstanceMethod("withDayOfYear", intP(DAY_OF_YEAR))
          .addInstanceMethod("withMonth", intP(MONTH_OF_YEAR))
          .addInstanceMethod("withYear", intP(YEAR))
          .build();

  private static final JavaTimeType LOCAL_DATE_TIME_APIS =
      JavaTimeType.builder()
          .setClassName("java.time.LocalDateTime")
          .addStaticMethod(
              "of",
              intP(YEAR),
              intP(MONTH_OF_YEAR),
              intP(DAY_OF_MONTH),
              intP(HOUR_OF_DAY),
              intP(MINUTE_OF_HOUR))
          .addStaticMethod(
              "of",
              intP(YEAR),
              monthP(MONTH_OF_YEAR),
              intP(DAY_OF_MONTH),
              intP(HOUR_OF_DAY),
              intP(MINUTE_OF_HOUR))
          .addStaticMethod(
              "of",
              intP(YEAR),
              intP(MONTH_OF_YEAR),
              intP(DAY_OF_MONTH),
              intP(HOUR_OF_DAY),
              intP(MINUTE_OF_HOUR),
              intP(SECOND_OF_MINUTE))
          .addStaticMethod(
              "of",
              intP(YEAR),
              monthP(MONTH_OF_YEAR),
              intP(DAY_OF_MONTH),
              intP(HOUR_OF_DAY),
              intP(MINUTE_OF_HOUR),
              intP(SECOND_OF_MINUTE))
          .addStaticMethod(
              "of",
              intP(YEAR),
              intP(MONTH_OF_YEAR),
              intP(DAY_OF_MONTH),
              intP(HOUR_OF_DAY),
              intP(MINUTE_OF_HOUR),
              intP(SECOND_OF_MINUTE),
              intP(NANO_OF_SECOND))
          .addStaticMethod(
              "of",
              intP(YEAR),
              monthP(MONTH_OF_YEAR),
              intP(DAY_OF_MONTH),
              intP(HOUR_OF_DAY),
              intP(MINUTE_OF_HOUR),
              intP(SECOND_OF_MINUTE),
              intP(NANO_OF_SECOND))
          .addInstanceMethod("withDayOfMonth", intP(DAY_OF_MONTH))
          .addInstanceMethod("withDayOfYear", intP(DAY_OF_YEAR))
          .addInstanceMethod("withHour", intP(HOUR_OF_DAY))
          .addInstanceMethod("withMinute", intP(MINUTE_OF_HOUR))
          .addInstanceMethod("withMonth", intP(MONTH_OF_YEAR))
          .addInstanceMethod("withNano", intP(NANO_OF_SECOND))
          .addInstanceMethod("withSecond", intP(SECOND_OF_MINUTE))
          .addInstanceMethod("withYear", intP(YEAR))
          .build();

  private static final ImmutableMap<String, JavaTimeType> APIS =
      Maps.uniqueIndex(
          ImmutableList.of(
              LOCAL_TIME_APIS,
              LOCAL_DATE_APIS,
              LOCAL_DATE_TIME_APIS,
              DAY_OF_WEEK_APIS,
              MONTH_APIS,
              YEAR_APIS,
              MONTH_DAY_APIS,
              YEAR_MONTH_APIS),
          JavaTimeType::className);

  private static final Matcher<ExpressionTree> JAVA_MATCHER =
      anyOf(packageStartsWith("java."), packageStartsWith("tck.java."));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    // Allow the JDK to do whatever it wants (unit tests, etc.)
    if (JAVA_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    // Get the receiver of the method invocation and make sure it's not null
    Type receiverType = ASTHelpers.getReceiverType(tree);
    if (receiverType == null) {
      return Description.NO_MATCH;
    }
    // If the receiver is not one of our java.time types, then return early
    JavaTimeType type = APIS.get(receiverType.toString());
    if (type == null) {
      return Description.NO_MATCH;
    }

    // Otherwise, check the method matchers we have for that type
    for (MatcherWithUnits matcherWithUnits : type.methods()) {
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
        // we short-circuit the loop here; only 1 method matcher will ever match, so there's no
        // sense in checking the rest of them
        return Description.NO_MATCH;
      }
    }
    return Description.NO_MATCH;
  }
}
