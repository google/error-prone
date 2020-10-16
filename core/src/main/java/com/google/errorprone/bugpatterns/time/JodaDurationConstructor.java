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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.packageStartsWith;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;

/** Check for calls to new Duration(long). */
@BugPattern(
    name = "JodaDurationConstructor",
    summary = "Use of new Duration(long) is not allowed. Please use Duration.millis(long) instead.",
    explanation =
        "Joda-Time's 'new Duration(long)' constructor is ambiguous with respect to time units and "
            + "is frequently a source of bugs. Please use Duration.millis(long) instead. If your "
            + "Duration is better expressed in terms of other units, use standardSeconds(long), "
            + "standardMinutes(long), standardHours(long), or standardDays(long) instead.",
    severity = WARNING)
// TODO(kak): delete this once JodaConstructors is fully enabled
public final class JodaDurationConstructor extends BugChecker implements NewClassTreeMatcher {
  private static final Matcher<ExpressionTree> MATCHER =
      allOf(
          anyOf(
              Matchers.constructor().forClass("org.joda.time.Duration").withParameters("int"),
              Matchers.constructor().forClass("org.joda.time.Duration").withParameters("long")),
          // Allow usage by JodaTime itself
          not(packageStartsWith("org.joda.time")));

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    ExpressionTree millisArg = Iterables.getOnlyElement(tree.getArguments());
    SuggestedFix fix =
        SuggestedFix.replace(
            getStartPosition(tree),
            getStartPosition(millisArg),
            state.getSourceForNode(tree.getIdentifier()) + ".millis(");
    return describeMatch(tree, fix);
  }
}
