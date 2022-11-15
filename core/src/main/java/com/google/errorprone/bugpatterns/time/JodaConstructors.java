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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.packageStartsWith;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
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
 *   <li>{@code org.joda.time.Instant#Instant(long)}
 *   <li>{@code org.joda.time.DateTime#DateTime()}
 *   <li>{@code org.joda.time.DateTime#DateTime(org.joda.time.Chronology)}
 *   <li>{@code org.joda.time.DateTime#DateTime(org.joda.time.DateTimeZone)}
 * </ul>
 */
@BugPattern(
    summary = "Use of certain JodaTime constructors are not allowed.",
    explanation = "Use JodaTime's static factories instead of the ambiguous constructors.",
    severity = WARNING)
public final class JodaConstructors extends BugChecker implements NewClassTreeMatcher {
  private static final Matcher<ExpressionTree> SELF_USAGE = packageStartsWith("org.joda.time");

  private static final Matcher<ExpressionTree> DURATION_CTOR =
      constructor().forClass("org.joda.time.Duration").withParameters("long");

  private static final Matcher<ExpressionTree> INSTANT_CTOR_NO_ARG =
      constructor().forClass("org.joda.time.Instant").withNoParameters();

  private static final Matcher<ExpressionTree> INSTANT_CTOR_LONG_ARG =
      constructor().forClass("org.joda.time.Instant").withParameters("long");

  private static final Matcher<ExpressionTree> DATE_TIME_CTOR_NO_ARG =
      constructor().forClass("org.joda.time.DateTime").withNoParameters();

  private static final Matcher<ExpressionTree> DATE_TIME_CTORS_ONE_ARG =
      anyOf(
          constructor()
              .forClass("org.joda.time.DateTime")
              .withParameters("org.joda.time.Chronology"),
          constructor()
              .forClass("org.joda.time.DateTime")
              .withParameters("org.joda.time.DateTimeZone"));

  private final boolean matchEpochMillisConstructor;

  public JodaConstructors(ErrorProneFlags flags) {
    this.matchEpochMillisConstructor =
        flags.getBoolean("JodaConstructors:MatchEpochMillisConstructor").orElse(true);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    // Allow usage by JodaTime itself
    if (SELF_USAGE.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    // ban new Duration(long)
    if (DURATION_CTOR.matches(tree, state)) {
      SuggestedFix fix =
          SuggestedFix.replace(
              getStartPosition(tree),
              getStartPosition(getOnlyElement(tree.getArguments())),
              state.getSourceForNode(tree.getIdentifier()) + ".millis(");
      return describeMatch(tree, fix);
    }

    // ban new Instant()
    if (INSTANT_CTOR_NO_ARG.matches(tree, state)) {
      SuggestedFix fix = SuggestedFix.replace(tree, getIdentifierSource(tree, state) + ".now()");
      return describeMatch(tree, fix);
    }

    // ban new Instant(long)
    if (matchEpochMillisConstructor && INSTANT_CTOR_LONG_ARG.matches(tree, state)) {
      SuggestedFix fix =
          SuggestedFix.replace(
              getStartPosition(tree),
              getStartPosition(getOnlyElement(tree.getArguments())),
              getIdentifierSource(tree, state) + ".ofEpochMilli(");
      return describeMatch(tree, fix);
    }

    // ban new DateTime()
    if (DATE_TIME_CTOR_NO_ARG.matches(tree, state)) {
      SuggestedFix fix = SuggestedFix.replace(tree, getIdentifierSource(tree, state) + ".now()");
      return describeMatch(tree, fix);
    }

    // ban new DateTime(DateTimeZone) and new DateTime(Chronology)
    if (DATE_TIME_CTORS_ONE_ARG.matches(tree, state)) {
      SuggestedFix fix =
          SuggestedFix.replace(
              getStartPosition(tree),
              getStartPosition(getOnlyElement(tree.getArguments())),
              getIdentifierSource(tree, state) + ".now(");
      return describeMatch(tree, fix);
    }

    // otherwise, no match
    return Description.NO_MATCH;
  }

  private static String getIdentifierSource(NewClassTree tree, VisitorState state) {
    return state.getSourceForNode(tree.getIdentifier());
  }
}
