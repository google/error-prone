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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.time.Period;

/**
 * Bans calls to {@code Duration.from(temporalAmount)} where {@code temporalAmount} is a {@link
 * Period}.
 *
 * @author kak@google.com (Kurt Alfred Kluever)
 */
@BugPattern(
    name = "DurationFrom",
    summary = "Duration.from(Duration) returns itself; from(Period) throws a runtime exception.",
    explanation =
        "Duration.from(TemporalAmount) will always throw a UnsupportedTemporalTypeException when "
            + "passed a Period and return itself when passed a Duration.",
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class DurationFrom extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> DURATION_FROM =
      staticMethod().onClass("java.time.Duration").named("from");

  private static final Matcher<Tree> DURATION = isSameType("java.time.Duration");

  private static final Matcher<Tree> PERIOD = isSameType("java.time.Period");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (DURATION_FROM.matches(tree, state)) {
      ExpressionTree arg0 = tree.getArguments().get(0);
      if (PERIOD.matches(arg0, state)) {
        return describeMatch(tree);
      }
      if (DURATION.matches(arg0, state)) {
        return describeMatch(tree, SuggestedFix.replace(tree, state.getSourceForNode(arg0)));
      }
    }
    return Description.NO_MATCH;
  }
}
