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

import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * Reports an error when a Duration or Instant is incorrectly decomposed in order to call an API
 * which accepts a {@code <long, TimeUnit>} pair.
 */
@BugPattern(
    name = "DurationToLongTimeUnit",
    summary = "Unit mismatch when decomposing a Duration or Instant to call a <long, TimeUnit> API",
    severity = ERROR,
    providesFix = REQUIRES_HUMAN_ATTENTION)
// TODO(kak): we should probably rename this as it works for Instants/Timestamps too
public final class DurationToLongTimeUnit extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final String JAVA_DURATION = "java.time.Duration";
  private static final String JAVA_INSTANT = "java.time.Instant";

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
          .put(instanceMethod().onExactClass(JAVA_DURATION).named("getSeconds"), SECONDS)
          .put(instanceMethod().onExactClass(JAVA_DURATION).named("toMinutes"), MINUTES)
          .put(instanceMethod().onExactClass(JAVA_DURATION).named("toHours"), HOURS)
          .put(instanceMethod().onExactClass(JAVA_DURATION).named("toDays"), DAYS)
          .put(instanceMethod().onExactClass(JAVA_INSTANT).named("toEpochMilli"), MILLISECONDS)
          .put(instanceMethod().onExactClass(JAVA_INSTANT).named("getEpochSecond"), SECONDS)
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
          .build();

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    List<? extends ExpressionTree> arguments = tree.getArguments();
    if (arguments.size() >= 2) { // TODO(kak): Do we want to check varargs as well?
      for (int i = 0; i < arguments.size() - 1; i++) {

        Type type0 = ASTHelpers.getType(arguments.get(i));
        Type type1 = ASTHelpers.getType(arguments.get(i + 1));

        Type longType = state.getSymtab().longType;
        Type timeUnitType = state.getTypeFromString(TIME_UNIT);

        if (ASTHelpers.isSameType(type0, longType, state)
            && ASTHelpers.isSameType(type1, timeUnitType, state)) {
          Optional<TimeUnit> timeUnit = getTimeUnit(arguments.get(i + 1));
          if (timeUnit.isPresent()) {
            ExpressionTree arg0 = arguments.get(i);
            for (Entry<Matcher<ExpressionTree>, TimeUnit> entry : MATCHERS.entrySet()) {
              TimeUnit correctTimeUnit = entry.getValue();
              if (entry.getKey().matches(arg0, state) && timeUnit.get() != correctTimeUnit) {
                SuggestedFix fix =
                    SuggestedFix.builder()
                        // TODO(kak): Do we want to use static imports here?
                        .addImport(TIME_UNIT)
                        .replace(arguments.get(i + 1), "TimeUnit." + correctTimeUnit)
                        .build();
                return describeMatch(tree, fix);
              }
            }
          }
        }
      }
    }
    return Description.NO_MATCH;
  }

  private static Optional<TimeUnit> getTimeUnit(ExpressionTree timeUnit) {
    if (timeUnit instanceof IdentifierTree) { // e.g., SECONDS
      return Enums.getIfPresent(TimeUnit.class, ((IdentifierTree) timeUnit).getName().toString());
    }
    if (timeUnit instanceof MemberSelectTree) { // e.g., TimeUnit.SECONDS
      return Enums.getIfPresent(
          TimeUnit.class, ((MemberSelectTree) timeUnit).getIdentifier().toString());
    }
    return Optional.absent();
  }
}
