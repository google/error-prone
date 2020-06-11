/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.common.base.Verify.verify;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.common.base.Joiner;
import com.google.common.collect.Range;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.util.ArrayList;
import java.util.List;

/**
 * Warns against suspect looking calls to {@link java.util.Date} APIs. Noteably, {@code Date} uses:
 *
 * <ul>
 *   <li>1900-based years (negative values permitted)
 *   <li>0-based months (with rollover and negative values permitted)
 *   <li>1-based days (with rollover and negative values permitted)
 *   <li>0-based hours (with rollover and negative values permitted)
 *   <li>0-based minutes (with rollover and negative values permitted)
 *   <li>0-based seconds (with rollover and negative values permitted)
 * </ul>
 *
 * @author kak@google.com (Kurt Alfred Kluever)
 */
@BugPattern(
    name = "DateChecker",
    summary = "Warns against suspect looking calls to java.util.Date APIs",
    explanation =
        "java.util.Date uses 1900-based years, 0-based months, 1-based days, and 0-based"
            + " hours/minutes/seconds. Additionally, it allows for negative values or very large"
            + " values (which rollover).",
    severity = WARNING)
public final class DateChecker extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {
  private static final String DATE = "java.util.Date";

  private static final Matcher<ExpressionTree> CONSTRUCTORS =
      anyOf(
          constructor().forClass(DATE).withParameters("int", "int", "int"),
          constructor().forClass(DATE).withParameters("int", "int", "int", "int", "int"),
          constructor().forClass(DATE).withParameters("int", "int", "int", "int", "int", "int"));

  private static final Matcher<ExpressionTree> SET_YEAR =
      instanceMethod().onExactClass(DATE).named("setYear");
  private static final Matcher<ExpressionTree> SET_MONTH =
      instanceMethod().onExactClass(DATE).named("setMonth");
  private static final Matcher<ExpressionTree> SET_DAY =
      instanceMethod().onExactClass(DATE).named("setDate");
  private static final Matcher<ExpressionTree> SET_HOUR =
      instanceMethod().onExactClass(DATE).named("setHours");
  private static final Matcher<ExpressionTree> SET_MIN =
      instanceMethod().onExactClass(DATE).named("setMinutes");
  private static final Matcher<ExpressionTree> SET_SEC =
      instanceMethod().onExactClass(DATE).named("setSeconds");

  // permits years [1901, 2050] which seems ~reasonable
  private static final Range<Integer> YEAR_RANGE = Range.closed(1, 150);
  private static final Range<Integer> MONTH_RANGE = Range.closed(0, 11);
  private static final Range<Integer> DAY_RANGE = Range.closed(1, 31);
  private static final Range<Integer> HOUR_RANGE = Range.closed(0, 23);
  private static final Range<Integer> SEC_MIN_RANGE = Range.closed(0, 59);

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    List<String> errors = new ArrayList<>();
    if (tree.getArguments().size() == 1) {
      ExpressionTree arg0 = tree.getArguments().get(0);

      if (SET_YEAR.matches(tree, state)) {
        checkYear(arg0, errors);
      } else if (SET_MONTH.matches(tree, state)) {
        checkMonth(arg0, errors);
      } else if (SET_DAY.matches(tree, state)) {
        checkDay(arg0, errors);
      } else if (SET_HOUR.matches(tree, state)) {
        checkHours(arg0, errors);
      } else if (SET_MIN.matches(tree, state)) {
        checkMinutes(arg0, errors);
      } else if (SET_SEC.matches(tree, state)) {
        checkSeconds(arg0, errors);
      }
    }
    return errors.isEmpty()
        ? Description.NO_MATCH
        : buildDescription(tree).setMessage(getOnlyElement(errors)).build();
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    List<String> errors = new ArrayList<>();
    if (CONSTRUCTORS.matches(tree, state)) {
      List<? extends ExpressionTree> args = tree.getArguments();
      int numArgs = args.size();
      verify(
          numArgs >= 3 && numArgs <= 6,
          "Expected the constructor to have at least 3 and at most 6 arguments, but it had %s",
          numArgs);

      checkYear(args.get(0), errors);
      checkMonth(args.get(1), errors);
      checkDay(args.get(2), errors);
      if (numArgs > 4) {
        checkHours(args.get(3), errors);
        checkMinutes(args.get(4), errors);
      }
      if (numArgs > 5) {
        checkSeconds(args.get(5), errors);
      }
    }

    return errors.isEmpty()
        ? Description.NO_MATCH
        : buildDescription(tree)
            .setMessage(
                "This call to new Date(...) looks suspect for the following reason(s): "
                    + Joiner.on("  ").join(errors))
            .build();
  }

  private static void checkYear(ExpressionTree tree, List<String> errors) {
    checkBounds(tree, "1900-based year", YEAR_RANGE, errors);
  }

  private static void checkMonth(ExpressionTree tree, List<String> errors) {
    checkBounds(tree, "0-based month", MONTH_RANGE, errors);
  }

  private static void checkDay(ExpressionTree tree, List<String> errors) {
    // TODO(kak): we should also consider checking if the given day is valid for the given
    // month/year. E.g., Feb 30th is never valid, Feb 29th is sometimes valid, and Feb 28th is
    // always valid.
    checkBounds(tree, "day", DAY_RANGE, errors);
  }

  private static void checkHours(ExpressionTree tree, List<String> errors) {
    checkBounds(tree, "hours", HOUR_RANGE, errors);
  }

  private static void checkMinutes(ExpressionTree tree, List<String> errors) {
    checkBounds(tree, "minutes", SEC_MIN_RANGE, errors);
  }

  private static void checkSeconds(ExpressionTree tree, List<String> errors) {
    checkBounds(tree, "seconds", SEC_MIN_RANGE, errors);
  }

  private static void checkBounds(
      ExpressionTree tree, String type, Range<Integer> range, List<String> errors) {
    Integer value = ASTHelpers.constValue(tree, Integer.class);
    if (!range.contains(value)) {
      errors.add(String.format("The %s value (%s) is out of bounds %s.", type, value, range));
    }
  }
}
