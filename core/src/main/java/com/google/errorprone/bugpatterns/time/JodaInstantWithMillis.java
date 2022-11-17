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
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/** Check for calls to {@code instant.withMillis(long)}. */
@BugPattern(
    summary =
        "Use of instant.withMillis(long) is not allowed. Use Instant.ofEpochMilli(long) instead.",
    explanation =
        "Joda-Time's 'instant.withMillis(long)' method is often a source of bugs because it "
            + "doesn't mutate the current instance but rather returns a new immutable Instant "
            + "instance. Please use Instant.ofEpochMilli(long) instead.",
    severity = WARNING)
public final class JodaInstantWithMillis extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> MATCHER =
      Matchers.allOf(
          Matchers.instanceMethod()
              .onExactClass("org.joda.time.Instant")
              .named("withMillis")
              .withParameters("long"),
          // Allow usage by JodaTime itself
          Matchers.not(Matchers.packageStartsWith("org.joda.time")));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder builder = SuggestedFix.builder();
    String replacement = qualifyType(state, builder, "org.joda.time.Instant") + ".ofEpochMilli(";
    ExpressionTree millisArg = Iterables.getOnlyElement(tree.getArguments());

    builder.replace(getStartPosition(tree), getStartPosition(millisArg), replacement);
    return describeMatch(tree, builder.build());
  }
}
