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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.FieldMatchers.staticField;
import static com.google.errorprone.matchers.Matchers.anyOf;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ImportTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Checks for usages of dangerous {@code DateTimeConstants} constants. */
@BugPattern(
    name = "JodaDateTimeConstants",
    summary =
        "Usage of the `_PER_` constants in `DateTimeConstants` are problematic because they"
            + " encourage manual date/time math.",
    explanation =
        "Manual date/time math leads to overflows, unit mismatches, and weak typing. Prefer to use"
            + " strong types (e.g., `java.time.Duration` or `java.time.Instant`) and their APIs to"
            + " perform date/time math.",
    severity = WARNING)
public final class JodaDateTimeConstants extends BugChecker
    implements MemberSelectTreeMatcher, ImportTreeMatcher {

  private static final Pattern DATE_TIME_CONSTANTS_STATIC_IMPORT_REGEX =
      Pattern.compile("^org\\.joda\\.time\\.DateTimeConstants\\.[A-Z]+_PER_[A-Z]+$");

  private static final Matcher<ExpressionTree> DATE_TIME_CONSTANTS_MATCHER =
      anyOf(
          Stream.of(
                  "DAYS_PER_WEEK",
                  "HOURS_PER_DAY",
                  "HOURS_PER_WEEK",
                  "MILLIS_PER_DAY",
                  "MILLIS_PER_HOUR",
                  "MILLIS_PER_MINUTE",
                  "MILLIS_PER_SECOND",
                  "MILLIS_PER_WEEK",
                  "MINUTES_PER_DAY",
                  "MINUTES_PER_HOUR",
                  "MINUTES_PER_WEEK",
                  "SECONDS_PER_DAY",
                  "SECONDS_PER_HOUR",
                  "SECONDS_PER_MINUTE",
                  "SECONDS_PER_WEEK")
              .map(constant -> staticField("org.joda.time.DateTimeConstants", constant))
              .collect(toImmutableSet()));

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    return DATE_TIME_CONSTANTS_MATCHER.matches(tree, state) ? describeMatch(tree) : NO_MATCH;
  }

  @Override
  public Description matchImport(ImportTree tree, VisitorState state) {
    return tree.isStatic()
            && DATE_TIME_CONSTANTS_STATIC_IMPORT_REGEX
                .matcher(state.getSourceForNode(tree.getQualifiedIdentifier()))
                .matches()
        ? describeMatch(tree)
        : NO_MATCH;
  }
}
