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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/** Check for calls to {@code java.time} APIs that silently use the default system time-zone. */
@BugPattern(
    summary = "java.time APIs that silently use the default system time-zone are not allowed.",
    explanation =
        "Using APIs that silently use the default system time-zone is dangerous. "
            + "The default system time-zone can vary from machine to machine or JVM to JVM. "
            + "You must choose an explicit ZoneId.",
    severity = WARNING)
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
          .withNoParameters();

  private static final Matcher<ExpressionTree> IN_JAVA_TIME =
      Matchers.packageStartsWith("java.time");

  private static boolean matches(MethodInvocationTree tree) {
    if (!tree.getArguments().isEmpty()) {
      return false;
    }
    MethodSymbol symbol = ASTHelpers.getSymbol(tree);

    switch (symbol.getSimpleName().toString()) {
      case "now":
        return symbol.isStatic() && NOW_STATIC.contains(symbol.owner.getQualifiedName().toString());
      case "dateNow":
        return !symbol.isStatic()
            && DATE_NOW_INSTANCE.contains(symbol.owner.getQualifiedName().toString());
      case "systemDefaultZone":
        return symbol.isStatic()
            && symbol.owner.getQualifiedName().contentEquals("java.time.Clock");
      default:
        return false;
    }
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!matches(tree) || IN_JAVA_TIME.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    String idealReplacementCode = "ZoneId.of(\"America/Los_Angeles\")";

    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    String zoneIdName = SuggestedFixes.qualifyType(state, fixBuilder, "java.time.ZoneId");
    String replacementCode = zoneIdName + ".systemDefault()";

    // The method could be statically imported and have no receiver: if so, just swap out the whole
    // tree as opposed to surgically replacing the post-receiver part..
    ExpressionTree receiver = ASTHelpers.getReceiver(tree);
    // we special case Clock because the replacement isn't just an overload, but a new API entirely
    boolean systemDefaultZoneClockMethod = CLOCK_MATCHER.matches(tree, state);
    String replacementMethod =
        systemDefaultZoneClockMethod ? "system" : ASTHelpers.getSymbol(tree).name.toString();
    if (receiver != null) {
      fixBuilder.replace(
          state.getEndPosition(receiver),
          state.getEndPosition(tree),
          "." + replacementMethod + "(" + replacementCode + ")");
    } else {
      if (systemDefaultZoneClockMethod) {
        fixBuilder.addStaticImport("java.time.Clock.systemDefaultZone");
      }
      fixBuilder.replace(tree, replacementMethod + "(" + replacementCode + ")");
    }

    return buildDescription(tree)
        .setMessage(
            String.format(
                "%s.%s is not allowed because it silently uses the system default time-zone. You "
                    + "must pass an explicit time-zone (e.g., %s) to this method.",
                ASTHelpers.getSymbol(tree).owner.getSimpleName(),
                ASTHelpers.getSymbol(tree),
                idealReplacementCode))
        .addFix(fixBuilder.build())
        .build();
  }
}
