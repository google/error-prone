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
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Enums;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Reports an error when a Duration or Instant is incorrectly decomposed in order to call an API
 * which accepts a {@code <long, TimeUnit>} pair.
 */
@BugPattern(
    summary = "Unit mismatch when decomposing a Duration or Instant to call a <long, TimeUnit> API",
    severity = ERROR)
// TODO(kak): we should probably rename this as it works for Instants/Timestamps too
public final class DurationToLongTimeUnit extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final String JAVA_DURATION = "java.time.Duration";
  private static final String JAVA_INSTANT = "java.time.Instant";

  private static final String JAVA_DURATIONS = "com.google.common.time.Durations";
  private static final String JAVA_INSTANTS = "com.google.common.time.Instants";

  private static final String JODA_DURATION = "org.joda.time.Duration";
  private static final String JODA_RDURATION = "org.joda.time.ReadableDuration";
  private static final String JODA_RINSTANT = "org.joda.time.ReadableInstant";

  private static final String PROTO_DURATION = "com.google.protobuf.Duration";
  private static final String PROTO_TIMESTAMP = "com.google.protobuf.Timestamp";

  private static final String TIME_UNIT = "java.util.concurrent.TimeUnit";

  private static final ImmutableMap<Matcher<ExpressionTree>, TimeUnit> MATCHERS =
      new ImmutableMap.Builder<Matcher<ExpressionTree>, TimeUnit>()
          // JavaTime
          .put(instanceMethod().onExactClass(JAVA_DURATION).named("toNanos"), NANOSECONDS)
          .put(instanceMethod().onExactClass(JAVA_DURATION).named("toMillis"), MILLISECONDS)
          .put(instanceMethod().onExactClass(JAVA_DURATION).named("toSeconds"), SECONDS)
          .put(instanceMethod().onExactClass(JAVA_DURATION).named("getSeconds"), SECONDS)
          .put(instanceMethod().onExactClass(JAVA_DURATION).named("toMinutes"), MINUTES)
          .put(instanceMethod().onExactClass(JAVA_DURATION).named("toHours"), HOURS)
          .put(instanceMethod().onExactClass(JAVA_DURATION).named("toDays"), DAYS)
          .put(instanceMethod().onExactClass(JAVA_INSTANT).named("toEpochMilli"), MILLISECONDS)
          .put(instanceMethod().onExactClass(JAVA_INSTANT).named("getEpochSecond"), SECONDS)
          // com.google.common.time APIs
          .put(staticMethod().onClass(JAVA_DURATIONS).named("toNanosSaturated"), NANOSECONDS)
          .put(staticMethod().onClass(JAVA_DURATIONS).named("toMicros"), MICROSECONDS)
          .put(staticMethod().onClass(JAVA_DURATIONS).named("toMicrosSaturated"), MICROSECONDS)
          .put(staticMethod().onClass(JAVA_DURATIONS).named("toMillisSaturated"), MILLISECONDS)
          .put(staticMethod().onClass(JAVA_INSTANTS).named("toEpochNanos"), NANOSECONDS)
          .put(staticMethod().onClass(JAVA_INSTANTS).named("toEpochNanosSaturated"), NANOSECONDS)
          .put(staticMethod().onClass(JAVA_INSTANTS).named("toEpochMicros"), MICROSECONDS)
          .put(staticMethod().onClass(JAVA_INSTANTS).named("toEpochMicrosSaturated"), MICROSECONDS)
          .put(staticMethod().onClass(JAVA_INSTANTS).named("toEpochMillisSaturated"), MILLISECONDS)
          // JodaTime
          .put(instanceMethod().onExactClass(JODA_DURATION).named("getStandardSeconds"), SECONDS)
          .put(instanceMethod().onExactClass(JODA_DURATION).named("getStandardMinutes"), MINUTES)
          .put(instanceMethod().onExactClass(JODA_DURATION).named("getStandardHours"), HOURS)
          .put(instanceMethod().onExactClass(JODA_DURATION).named("getStandardDays"), DAYS)
          .put(instanceMethod().onDescendantOf(JODA_RDURATION).named("getMillis"), MILLISECONDS)
          .put(instanceMethod().onDescendantOf(JODA_RINSTANT).named("getMillis"), MILLISECONDS)
          // ProtoTime
          .put(instanceMethod().onExactClass(PROTO_DURATION).named("getSeconds"), SECONDS)
          .put(instanceMethod().onExactClass(PROTO_TIMESTAMP).named("getSeconds"), SECONDS)
          .buildOrThrow();

  private static final Matcher<ExpressionTree> TIME_UNIT_DECOMPOSITION =
      instanceMethod().onExactClass(TIME_UNIT).named("convert").withParameters(JAVA_DURATION);

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    List<? extends ExpressionTree> arguments = tree.getArguments();

    if (arguments.size() >= 2) { // TODO(kak): Do we want to check varargs as well?
      Type longType = state.getSymtab().longType;
      Type timeUnitType = JAVA_UTIL_CONCURRENT_TIMEUNIT.get(state);
      for (int i = 0; i < arguments.size() - 1; i++) {
        ExpressionTree arg0 = arguments.get(i);
        ExpressionTree timeUnitTree = arguments.get(i + 1);

        Type type0 = ASTHelpers.getType(arg0);
        Type type1 = ASTHelpers.getType(timeUnitTree);
        if (ASTHelpers.isSameType(type0, longType, state)
            && ASTHelpers.isSameType(type1, timeUnitType, state)) {
          Optional<TimeUnit> timeUnitInArgument = getTimeUnit(timeUnitTree);
          if (timeUnitInArgument.isPresent()) {

            for (Map.Entry<Matcher<ExpressionTree>, TimeUnit> entry : MATCHERS.entrySet()) {
              TimeUnit timeUnitExpressedInConversion = entry.getValue();
              if (entry.getKey().matches(arg0, state)
                  && timeUnitInArgument.get() != timeUnitExpressedInConversion) {
                return describeTimeUnitConversionFix(
                    tree, timeUnitTree, timeUnitExpressedInConversion);
              }
            }

            // Look for TimeUnit.convert(Duration), dynamically looking for the source time unit
            // to convert from.
            if (TIME_UNIT_DECOMPOSITION.matches(arg0, state)) {
              Optional<TimeUnit> sourceTimeUnit = getTimeUnit(ASTHelpers.getReceiver(arg0));
              if (sourceTimeUnit.isPresent()
                  && !sourceTimeUnit.get().equals(timeUnitInArgument.get())) {
                return describeTimeUnitConversionFix(tree, timeUnitTree, sourceTimeUnit.get());
              }
            }
          }
        }
      }
    }
    return Description.NO_MATCH;
  }

  private Description describeTimeUnitConversionFix(
      MethodInvocationTree tree,
      ExpressionTree timeUnitTree,
      TimeUnit timeUnitExpressedInConversion) {
    SuggestedFix fix =
        SuggestedFix.builder()
            // TODO(kak): Do we want to use static imports here?
            .addImport(TIME_UNIT)
            .replace(timeUnitTree, "TimeUnit." + timeUnitExpressedInConversion)
            .build();
    return describeMatch(tree, fix);
  }

  /**
   * Given that this ExpressionTree's type is a {@link java.util.concurrent.TimeUnit}, return the
   * TimeUnit it likely represents.
   */
  static Optional<TimeUnit> getTimeUnit(ExpressionTree timeUnit) {
    if (timeUnit instanceof IdentifierTree identifierTree) { // e.g., SECONDS
      return Enums.getIfPresent(TimeUnit.class, identifierTree.getName().toString()).toJavaUtil();
    }
    if (timeUnit instanceof MemberSelectTree memberSelectTree) { // e.g., TimeUnit.SECONDS
      return Enums.getIfPresent(TimeUnit.class, memberSelectTree.getIdentifier().toString())
          .toJavaUtil();
    }
    return Optional.empty();
  }

  private static final Supplier<Type> JAVA_UTIL_CONCURRENT_TIMEUNIT =
      VisitorState.memoize(state -> state.getTypeFromString(TIME_UNIT));
}
