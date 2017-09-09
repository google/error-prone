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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.TimeZone;
import java.util.regex.Pattern;

/** @author awturner@google.com (Andy Turner) */
@BugPattern(
  name = "InvalidTimeZoneID",
  summary =
      "Invalid time zone identifier. TimeZone.getTimeZone(String) will silently return GMT instead "
          + "of the time zone you intended.",
  explanation =
      "TimeZone.getTimeZone(String) silently returns GMT when an invalid time zone identifier is "
          + "passed in.",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class InvalidTimeZoneID extends BugChecker implements MethodInvocationTreeMatcher {
  private static final ImmutableSet<String> AVAILABLE_IDS =
      ImmutableSet.copyOf(TimeZone.getAvailableIDs());

  private static final Matcher<ExpressionTree> METHOD_MATCHER =
      Matchers.methodInvocation(
          MethodMatchers.staticMethod()
              .onClass("java.util.TimeZone")
              .withSignature("getTimeZone(java.lang.String)"));

  // https://docs.oracle.com/javase/8/docs/api/java/util/TimeZone.html
  // "a custom time zone ID can be specified to produce a TimeZone".
  // 0-23, with optional leading zero.
  private static final String HOURS_PATTERN = "([0-9]|[0-1][0-9]|2[0-3])";
  // 00-59, optional.
  private static final String MINUTES_PATTERN = "(?:[0-5][0-9])?";
  private static final Pattern CUSTOM_ID_PATTERN =
      Pattern.compile("GMT[+\\-]" + HOURS_PATTERN + ":?" + MINUTES_PATTERN);

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, final VisitorState state) {
    if (!METHOD_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    String value = (String) ASTHelpers.constValue(tree.getArguments().get(0));
    if (value == null) {
      // Value isn't a compile-time constant, so we can't know if it's unsafe.
      return Description.NO_MATCH;
    }
    if (isValidID(value)) {
      // Value is supported on this JVM.
      return Description.NO_MATCH;
    }

    // Value is invalid, so let's suggest some alternatives.
    Description.Builder builder = buildDescription(tree);

    // Try to see if it's just been mistyped with spaces instead of underscores - if so, offer this
    // as a potential fix.
    String spacesToUnderscores = value.replace(' ', '_');
    if (isValidID(spacesToUnderscores)) {
      builder.addFix(
          SuggestedFix.replace(
              tree.getArguments().get(0), String.format("\"%s\"", spacesToUnderscores)));
    }

    return builder.build();
  }

  private static boolean isValidID(String value) {
    if (AVAILABLE_IDS.contains(value)) {
      // Value is in TimeZone.getAvailableIDs(), so it's supported on this JVM.
      return true;
    }
    if (CUSTOM_ID_PATTERN.matcher(value).matches()) {
      // Value is a custom ID, so it's supported.
      return true;
    }
    return false;
  }
}
