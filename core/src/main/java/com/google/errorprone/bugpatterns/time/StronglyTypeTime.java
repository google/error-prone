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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.StronglyType;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import java.util.regex.Pattern;

/** Flags fields which would be better expressed as time types rather than primitive integers. */
@BugPattern(
    summary =
        "This primitive integral type is only used to construct time types. It would be clearer to"
            + " strongly type the field instead.",
    severity = WARNING)
public final class StronglyTypeTime extends BugChecker implements CompilationUnitTreeMatcher {
  private static final Matcher<ExpressionTree> TIME_FACTORY =
      anyOf(
          // Java time.
          staticMethod()
              .onClass("java.time.Duration")
              .namedAnyOf("ofNanos", "ofMillis", "ofSeconds", "ofMinutes", "ofHours", "ofDays")
              .withParameters("long"),
          staticMethod()
              .onClass("java.time.Instant")
              .namedAnyOf("ofEpochMilli", "ofEpochSecond")
              .withParameters("long"),
          // Proto time.
          staticMethod()
              .onClass("com.google.protobuf.util.Timestamps")
              .namedAnyOf("fromNanos", "fromMicros", "fromMillis", "fromSeconds"),
          staticMethod()
              .onClass("com.google.protobuf.util.Durations")
              .namedAnyOf(
                  "fromNanos",
                  "fromMicros",
                  "fromMillis",
                  "fromSeconds",
                  "fromMinutes",
                  "fromHours",
                  "fromDays"),
          // Joda time.
          staticMethod()
              .onClass("org.joda.time.Duration")
              .namedAnyOf(
                  "millis", "standardSeconds", "standardMinutes", "standardHours", "standardDays")
              .withParameters("long"),
          constructor().forClass("org.joda.time.Instant").withParameters("long"),
          constructor().forClass("org.joda.time.DateTime").withParameters("long"));

  private static final Pattern TIME_UNIT_REMOVER =
      Pattern.compile(
          "((_?IN)?_?(NANO|NANOSECOND|NSEC|_NS|MICRO|MSEC|USEC|MICROSECOND|MILLI|MILLISECOND|_MS|SEC|SECOND|MINUTE|MIN|HOUR|DAY)S?)?$",
          Pattern.CASE_INSENSITIVE);

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    return StronglyType.forCheck(this)
        .addType(state.getSymtab().intType)
        .addType(state.getSymtab().longType)
        .addType(state.getSymtab().floatType)
        .addType(state.getSymtab().doubleType)
        .setFactoryMatcher(TIME_FACTORY)
        .setRenameFunction(StronglyTypeTime::createNewName)
        .build()
        .match(tree, state);
  }

  /** Tries to strip any time-related suffix off the field name. */
  private static final String createNewName(String fieldName) {
    String newName = TIME_UNIT_REMOVER.matcher(fieldName).replaceAll("");
    // Guard against field names that *just* contain the unit. Not much we can do here.
    return newName.isEmpty() ? fieldName : newName;
  }
}
