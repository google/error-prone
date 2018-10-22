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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.fixes.SuggestedFixes;

/** Check for calls to {@code java.time} APIs that silently use the default system time-zone. */
@BugPattern(
    name = "JavaTimeDefaultTimeZone",
    summary = "java.time APIs that silently use the default system time-zone are not allowed.",
    explanation =
        "Using APIs that silently use the default system time-zone is dangerous. "
            + "The default system time-zone can vary from machine to machine or JVM to JVM. "
            + "You must choose an explicit ZoneId."
    ,
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class JavaTimeDefaultTimeZone extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final ImmutableSet<String> NOW_STATIC =
      ImmutableSet.of(
          "java.time.LocalDate",
          "java.time.LocalDateTime",
          "java.time.LocalTime",
          "java.time.MonthDay",
          "java.time.OffsetDateTime",
          "java.time.OffsetTime",
          "java.time.Year",
          "java.time.YearMonth",
          "java.time.ZonedDateTime",
          "java.time.chrono.JapaneseDate",
          "java.time.chrono.MinguoDate",
          "java.time.chrono.HijrahDate",
          "java.time.chrono.ThaiBuddhistDate");

  private static final ImmutableSet<String> DATE_NOW_INSTANCE =
      ImmutableSet.of(
          "java.time.chrono.Chronology",
          "java.time.chrono.HijrahChronology",
          "java.time.chrono.IsoChronology",
          "java.time.chrono.JapaneseChronology",
          "java.time.chrono.MinguoChronology",
          "java.time.chrono.ThaiBuddhistChronology");

  private static final Matcher<ExpressionTree> CLOCK_MATCHER =
      Matchers.staticMethod()
          .onClass("java.time.Clock")
          .named("systemDefaultZone")
          .withParameters();

  private static final Matcher<ExpressionTree> MATCHER =
      Matchers.allOf(
          buildMatcher(),
          // Allow usage by java.time itself
          Matchers.not(Matchers.packageStartsWith("java.time")));

  private static Matcher<ExpressionTree> buildMatcher() {
    List<Matcher<ExpressionTree>> matchers = new ArrayList<>();
    for (String type : NOW_STATIC) {
      matchers.add(Matchers.staticMethod().onClass(type).named("now").withParameters());
    }
    for (String type : DATE_NOW_INSTANCE) {
      matchers.add(Matchers.instanceMethod().onExactClass(type).named("dateNow").withParameters());
    }
    matchers.add(CLOCK_MATCHER);
    return Matchers.anyOf(matchers);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    MethodSymbol method = ASTHelpers.getSymbol(tree);
    String replacementMethod = method.name.toString();

    // we special case Clock because the replacement isn't just an overload, but a new API entirely
    if (CLOCK_MATCHER.matches(tree, state)) {
      replacementMethod = "system";
    }

    String idealReplacementCode = "ZoneId.of(\"America/Los_Angeles\")";

    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    String zoneIdName = SuggestedFixes.qualifyType(state, fixBuilder, "java.time.ZoneId");
    String replacementCode = zoneIdName + ".systemDefault()";

    fixBuilder.replace(
        state.getEndPosition(ASTHelpers.getReceiver(tree)),
        state.getEndPosition(tree),
        "." + replacementMethod + "(" + replacementCode + ")");

    return buildDescription(tree)
        .setMessage(
            String.format(
                "%s.%s is not allowed because it silently uses the system default time-zone. You "
                    + "must pass an explicit time-zone (e.g., %s) to this method.",
                method.owner.getSimpleName(), method, idealReplacementCode))
        .addFix(fixBuilder.build())
        .build();
  }
}
