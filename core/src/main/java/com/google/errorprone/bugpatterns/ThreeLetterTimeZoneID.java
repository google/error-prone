/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.time.ZoneId;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/** @author awturner@google.com (Andy Turner) */
@BugPattern(
  name = "ThreeLetterTimeZoneID",
  summary = ThreeLetterTimeZoneID.SUMMARY,
  category = JDK,
  severity = WARNING,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class ThreeLetterTimeZoneID extends BugChecker implements MethodInvocationTreeMatcher {
  static final String SUMMARY =
      "Three-letter time zone identifiers are deprecated, may be ambiguous, and might not do what "
          + "you intend; the full IANA time zone ID should be used instead.";

  private static final Matcher<ExpressionTree> METHOD_MATCHER =
      MethodMatchers.staticMethod()
          .onClass("java.util.TimeZone")
          .withSignature("getTimeZone(java.lang.String)");

  private static final Matcher<ExpressionTree> JODATIME_METHOD_MATCHER =
      MethodMatchers.staticMethod()
          .onClass("org.joda.time.DateTimeZone")
          .withSignature("forTimeZone(java.util.TimeZone)");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, final VisitorState state) {
    if (!METHOD_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    String value = ASTHelpers.constValue(tree.getArguments().get(0), String.class);
    if (value == null) {
      // Value isn't a compile-time constant, so we can't know if it's unsafe.
      return Description.NO_MATCH;
    }
    Replacement replacement = getReplacement(value, isInJodaTimeContext(state));
    if (replacement.replacements.isEmpty()) {
      return Description.NO_MATCH;
    }

    Description.Builder builder = buildDescription(tree).setMessage(replacement.message);
    for (String r : replacement.replacements) {
      builder.addFix(
          SuggestedFix.replace(
              tree.getArguments().get(0), state.getTreeMaker().Literal(r).toString()));
    }
    return builder.build();
  }

  static Replacement getReplacement(String id, boolean inJodaTimeContext) {
    switch (id) {
      case "EST":
        return handleNonDaylightSavingsZone(inJodaTimeContext, "America/New_York", "Etc/GMT+5");
      case "HST":
        return handleNonDaylightSavingsZone(inJodaTimeContext, "Pacific/Honolulu", "Etc/GMT+10");
      case "MST":
        return handleNonDaylightSavingsZone(inJodaTimeContext, "America/Denver", "Etc/GMT+7");
      default:
        // Fall through, we will handle it below.
    }

    String zoneIdReplacement = ZoneId.SHORT_IDS.get(id);
    if (zoneIdReplacement == null) {
      return Replacement.NO_REPLACEMENT;
    }

    if (id.endsWith("ST")) {
      TimeZone timeZone = TimeZone.getTimeZone(id);
      if (timeZone.observesDaylightTime()) {
        // Make sure that the offset is a whole number of hours; otherwise, there is no Etc/GMT+X
        // zone. Custom time zones don't need to be handled.
        long hours = TimeUnit.MILLISECONDS.toHours(timeZone.getRawOffset());
        long millis = TimeUnit.HOURS.toMillis(hours);
        if (millis == timeZone.getRawOffset()) {
          // This is a "X Standard Time" zone, but it observes daylight savings.
          // Suggest the equivalent zone, as well as a fixed zone at the non-daylight savings
          // offset.
          String fixedOffset = String.format("Etc/GMT%+d", -hours);
          String newDescription =
              SUMMARY
                  + "\n\n"
                  + observesDaylightSavingsMessage("TimeZone", zoneIdReplacement, fixedOffset);
          return new Replacement(newDescription, ImmutableList.of(zoneIdReplacement, fixedOffset));
        }
      }
    }
    return new Replacement(SUMMARY, ImmutableList.of(zoneIdReplacement));
  }

  // American time zones for which the TLA doesn't observe daylight savings.
  // http://www-01.ibm.com/support/docview.wss?uid=swg21250503#3char
  // How we handle it depends upon whether we are in a JodaTime context or not.
  static Replacement handleNonDaylightSavingsZone(
      boolean inJodaTimeContext, String daylightSavingsZone, String fixedOffset) {
    if (inJodaTimeContext) {
      String newDescription =
          SUMMARY
              + "\n\n"
              + observesDaylightSavingsMessage("DateTimeZone", daylightSavingsZone, fixedOffset);
      return new Replacement(newDescription, ImmutableList.of(daylightSavingsZone, fixedOffset));
    } else {
      String newDescription =
          SUMMARY
              + "\n\n"
              + "This TimeZone will not observe daylight savings. "
              + "If this is intended, use "
              + fixedOffset
              + " instead; to observe daylight savings, use "
              + daylightSavingsZone
              + ".";
      return new Replacement(newDescription, ImmutableList.of(fixedOffset, daylightSavingsZone));
    }
  }

  private static String observesDaylightSavingsMessage(
      String type, String daylightSavingsZone, String fixedOffset) {
    return "This "
        + type
        + " will observe daylight savings. "
        + "If this is intended, use "
        + daylightSavingsZone
        + " instead; otherwise use "
        + fixedOffset
        + ".";
  }

  private static boolean isInJodaTimeContext(VisitorState state) {
    if (state.getPath().getParentPath() != null) {
      Tree parentLeaf = state.getPath().getParentPath().getLeaf();
      if (parentLeaf instanceof ExpressionTree
          && JODATIME_METHOD_MATCHER.matches((ExpressionTree) parentLeaf, state)) {
        return true;
      }
    }
    return false;
  }

  @VisibleForTesting
  static final class Replacement {
    static final Replacement NO_REPLACEMENT = new Replacement("", ImmutableList.of());

    final String message;
    final ImmutableList<String> replacements;

    Replacement(String message, ImmutableList<String> replacements) {
      this.message = message;
      this.replacements = replacements;
    }
  }
}
