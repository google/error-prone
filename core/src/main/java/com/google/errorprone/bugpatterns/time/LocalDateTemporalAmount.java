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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.packageStartsWith;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Bans calls to {@code LocalDate.plus(TemporalAmount)} and {@code LocalDate.minus(TemporalAmount)}
 * where the {@link java.time.temporal.TemporalAmount} is a non-zero {@link java.time.Duration}.
 */
@BugPattern(
    summary =
        "LocalDate.plus() and minus() does not work with Durations. LocalDate represents civil"
            + " time (years/months/days), so java.time.Period is the appropriate thing to add or"
            + " subtract instead.",
    severity = ERROR)
public final class LocalDateTemporalAmount extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<MethodInvocationTree> MATCHER =
      allOf(
          instanceMethod()
              .onExactClass("java.time.LocalDate")
              .namedAnyOf("plus", "minus")
              .withParameters("java.time.temporal.TemporalAmount"),
          // TODO(kak): If the parameter is equal to Duration.ZERO, then technically this call will
          // work, but that's a very small corner case to worry about.
          argument(0, isSameType("java.time.Duration")),
          not(packageStartsWith("java.")));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return MATCHER.matches(tree, state) ? describeMatch(tree) : Description.NO_MATCH;
  }
}
