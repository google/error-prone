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
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.primitives.Longs;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.util.concurrent.TimeUnit;

/** Check for problematic or suspicious TimeUnit conversion calls. */
@BugPattern(
    name = "TimeUnitConversionChecker",
    summary =
        "This TimeUnit conversion looks buggy: converting from a smaller unit to a larger unit "
            + "(and passing a constant), converting to/from the same TimeUnit, or converting "
            + "TimeUnits where the result is statically known to be 0 or 1 are all buggy patterns.",
    explanation =
        "This checker flags potential problems with TimeUnit conversions: "
            + "1) conversions that are statically known to be equal to 0 or 1; "
            + "2) conversions that are converting from a given unit back to the same unit; "
            + "3) conversions that are converting from a smaller unit to a larger unit and passing "
            + "a constant value",
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class TimeUnitConversionChecker extends BugChecker
    implements MethodInvocationTreeMatcher {

  // TODO(kak): We should probably also extend this to recognize TimeUnit.convertTo() invocations

  private static final Matcher<ExpressionTree> MATCHER =
      instanceMethod()
          .onExactClass("java.util.concurrent.TimeUnit")
          .namedAnyOf(
              "toDays", "toHours", "toMinutes", "toSeconds", "toMillis", "toMicros", "toNanos");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    Tree receiverOfConversion = ASTHelpers.getReceiver(tree);
    if (receiverOfConversion == null) {
      // Usage inside TimeUnit itself, no changes we can make here.
      return Description.NO_MATCH;
    }
    // This trips up on code like:
    // TimeUnit SECONDS = TimeUnit.MINUTES;
    // long about2500 = SECONDS.toSeconds(42);
    // but... I think that's bad enough to ignore here :)
    String timeUnitName = ASTHelpers.getSymbol(receiverOfConversion).getSimpleName().toString();
    Optional<TimeUnit> receiver = Enums.getIfPresent(TimeUnit.class, timeUnitName);
    if (!receiver.isPresent()) {
      return Description.NO_MATCH;
    }

    String methodName = ASTHelpers.getSymbol(tree).getSimpleName().toString();
    TimeUnit convertTo = methodNameToTimeUnit(methodName);
    ExpressionTree arg0 = tree.getArguments().get(0);

    // if we have a constant and can Long-parse it...
    Long constant = Longs.tryParse(String.valueOf(state.getSourceForNode(arg0)));
    if (constant != null) {
      long converted = invokeConversion(receiver.get(), methodName, constant);

      // ... and the conversion results in 0 or 1, just inline it!
      if (converted == 0 || converted == 1 || constant == converted) {
        SuggestedFix fix = replaceTreeWith(tree, convertTo, converted + "L");
        return describeMatch(tree, fix);
      }

      // otherwise we have a suspect case: SMALLER_UNIT.toLargerUnit(constantValue)
      // because: "people usually don't like to have constants like 60_000_000 and use"
      //          "libraries to turn them into smaller numbers"
      if (receiver.get().compareTo(convertTo) < 0) {
        // We can't suggest a replacement here, so we just have to error out.
        return describeMatch(tree);
      }
    }

    // if we're trying to convert the unit to itself, just return the arg
    if (receiver.get().equals(convertTo)) {
      SuggestedFix fix = replaceTreeWith(tree, convertTo, state.getSourceForNode(arg0));
      return describeMatch(tree, fix);
    }
    return Description.NO_MATCH;
  }

  private static SuggestedFix replaceTreeWith(
      MethodInvocationTree tree, TimeUnit units, String replacement) {
    return SuggestedFix.builder()
        .postfixWith(tree, " /* " + units.toString().toLowerCase() + " */")
        .replace(tree, replacement)
        .build();
  }

  private static long invokeConversion(TimeUnit timeUnit, String methodName, long duration) {
    switch (methodName) {
      case "toDays":
        return timeUnit.toDays(duration);
      case "toHours":
        return timeUnit.toHours(duration);
      case "toMinutes":
        return timeUnit.toMinutes(duration);
      case "toSeconds":
        return timeUnit.toSeconds(duration);
      case "toMillis":
        return timeUnit.toMillis(duration);
      case "toMicros":
        return timeUnit.toMicros(duration);
      case "toNanos":
        return timeUnit.toNanos(duration);
      default:
        throw new IllegalArgumentException();
    }
  }

  private static TimeUnit methodNameToTimeUnit(String methodName) {
    switch (methodName) {
      case "toDays":
        return TimeUnit.DAYS;
      case "toHours":
        return TimeUnit.HOURS;
      case "toMinutes":
        return TimeUnit.MINUTES;
      case "toSeconds":
        return TimeUnit.SECONDS;
      case "toMillis":
        return TimeUnit.MILLISECONDS;
      case "toMicros":
        return TimeUnit.MICROSECONDS;
      case "toNanos":
        return TimeUnit.NANOSECONDS;
      default:
        throw new IllegalArgumentException();
    }
  }
}
