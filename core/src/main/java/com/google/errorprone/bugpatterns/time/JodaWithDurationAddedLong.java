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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.packageStartsWith;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Check for calls to JodaTime's {@code type.withDurationAdded(long, int)} where {@code <type> =
 * {Duration,Instant,DateTime}}.
 */
@BugPattern(
    name = "JodaWithDurationAddedLong",
    summary =
        "Use of JodaTime's type.withDurationAdded(long, int) (where <type> = "
            + "{Duration,Instant,DateTime}). Please use "
            + "type.withDurationAdded(Duration.millis(long), int) instead.",
    explanation =
        "JodaTime's type.withDurationAdded(long, int) is often a source of bugs "
            + "because the units of the parameters are ambiguous. Please use "
            + "type.withDurationAdded(Duration.millis(long), int) instead.",
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class JodaWithDurationAddedLong extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      allOf(
          anyOf(
              instanceMethod()
                  .onExactClass("org.joda.time.DateTime")
                  .named("withDurationAdded")
                  .withParameters("long", "int"),
              instanceMethod()
                  .onExactClass("org.joda.time.Duration")
                  .named("withDurationAdded")
                  .withParameters("long", "int"),
              instanceMethod()
                  .onExactClass("org.joda.time.Instant")
                  .named("withDurationAdded")
                  .withParameters("long", "int")),
          // Allow usage by JodaTime itself
          not(packageStartsWith("org.joda.time")));

  private static final Matcher<ExpressionTree> DURATION_GET_MILLIS_MATCHER =
      Matchers.methodInvocation(
          MethodMatchers.instanceMethod()
              .onDescendantOf("org.joda.time.ReadableDuration")
              .named("getMillis"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    String receiver = state.getSourceForNode(ASTHelpers.getReceiver(tree));

    // Get the constant scalar, if there is one.
    Integer scalar = ASTHelpers.constValue(tree.getArguments().get(1), Integer.class);

    SuggestedFix.Builder builder = SuggestedFix.builder();

    if (Integer.valueOf(0).equals(scalar)) {
      // This is adding zero times a duration to the receiver: we can replace this with just the
      // receiver.
      builder.replace(tree, receiver);
    } else {
      ExpressionTree firstArgumentTree = tree.getArguments().get(0);
      String firstArgumentReplacement;
      if (DURATION_GET_MILLIS_MATCHER.matches(firstArgumentTree, state)) {
        // This is passing {@code someDuration.getMillis()} as the parameter. we can replace this
        // with {@code someDuration}.
        firstArgumentReplacement =
            state.getSourceForNode(ASTHelpers.getReceiver(firstArgumentTree));
      } else {
        // Wrap the long as a Duration.
        firstArgumentReplacement =
            SuggestedFixes.qualifyType(state, builder, "org.joda.time.Duration")
                + ".millis("
                + state.getSourceForNode(firstArgumentTree)
                + ")";
      }

      if (Integer.valueOf(1).equals(scalar)) {
        // Use plus instead of adding 1 times the duration.
        builder.replace(tree, receiver + ".plus(" + firstArgumentReplacement + ")");
      } else if (Integer.valueOf(-1).equals(scalar)) {
        // Use minus instead of adding -1 times the duration.
        builder.replace(tree, receiver + ".minus(" + firstArgumentReplacement + ")");
      } else {
        builder.replace(firstArgumentTree, firstArgumentReplacement);
      }
    }
    return describeMatch(tree, builder.build());
  }
}
