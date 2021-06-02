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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
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
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;

/**
 * Check for calls to bad JodaTime constructors.
 *
 * <ul>
 *   <li>{@code org.joda.time.Duration#Duration(long)}
 *   <li>{@code org.joda.time.Instant#Instant()}
 *   <li>{@code org.joda.time.DateTime#DateTime()}
 *   <li>{@code org.joda.time.DateTime#DateTime(org.joda.time.Chronology)}
 *   <li>{@code org.joda.time.DateTime#DateTime(org.joda.time.DateTimeZone)}
 * </ul>
 */
@BugPattern(
    name = "JodaConstructors",
    summary = "Use of certain JodaTime constructors are not allowed.",
    explanation = "Use JodaTime's static factories instead of the ambiguous constructors.",
    severity = WARNING)
public final class JodaConstructors extends BugChecker implements NewClassTreeMatcher {
  private static final Matcher<ExpressionTree> SELF_USAGE = packageStartsWith("org.joda.time");

  private static final Matcher<ExpressionTree> DURATION_CTOR =
      constructor().forClass("org.joda.time.Duration").withParameters("long");

  private static final Matcher<ExpressionTree> INSTANT_CTOR =
      constructor().forClass("org.joda.time.Instant").withParameters();

  private static final Matcher<ExpressionTree> DATE_TIME_CTORS =
      anyOf(
          constructor().forClass("org.joda.time.DateTime").withParameters(),
          constructor()
              .forClass("org.joda.time.DateTime")
              .withParameters("org.joda.time.Chronology"),
          constructor()
              .forClass("org.joda.time.DateTime")
              .withParameters("org.joda.time.DateTimeZone"));

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    // Allow usage by JodaTime itself
    if (SELF_USAGE.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    // ban new Duration(long)
    if (DURATION_CTOR.matches(tree, state)) {
      ExpressionTree millisArg = Iterables.getOnlyElement(tree.getArguments());
      SuggestedFix fix =
          SuggestedFix.replace(
              getStartPosition(tree),
              getStartPosition(millisArg),
              state.getSourceForNode(tree.getIdentifier()) + ".millis(");
      return describeMatch(tree, fix);
    }

    // ban new Instant()
    if (INSTANT_CTOR.matches(tree, state)) {
      SuggestedFix fix =
          SuggestedFix.replace(tree, state.getSourceForNode(tree.getIdentifier()) + ".now()");
      return describeMatch(tree, fix);
    }

    // ban new DateTime(), new DateTime(DateTimeZone), and new DateTime(Chronology)
    if (DATE_TIME_CTORS.matches(tree, state)) {
      if (tree.getArguments().isEmpty()) {
        SuggestedFix fix =
            SuggestedFix.replace(tree, state.getSourceForNode(tree.getIdentifier()) + ".now()");
        return describeMatch(tree, fix);
      } else {
        ExpressionTree arg = Iterables.getOnlyElement(tree.getArguments());
        SuggestedFix fix =
            SuggestedFix.replace(
                getStartPosition(tree),
                getStartPosition(arg),
                state.getSourceForNode(tree.getIdentifier()) + ".now(");
        return describeMatch(tree, fix);
      }
    }

    // otherwise, no match
    return Description.NO_MATCH;
  }
}
