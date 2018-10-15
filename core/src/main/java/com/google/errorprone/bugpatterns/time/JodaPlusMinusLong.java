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

import com.google.common.collect.ImmutableSet;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Check for calls to JodaTime's {@code type.plus(long)} and {@code type.minus(long)} where {@code
 * <type> = {Duration,Instant,DateTime,DateMidnight}}.
 */
@BugPattern(
    name = "JodaPlusMinusLong",
    summary =
        "Use of JodaTime's type.plus(long) or type.minus(long) is not allowed (where <type> = "
            + "{Duration,Instant,DateTime,DateMidnight}). Please use "
            + " type.plus(Duration.millis(long)) or type.minus(Duration.millis(long)) instead.",
    explanation =
        "JodaTime's type.plus(long) and type.minus(long) methods are often a source of bugs "
            + "because the units of the parameters are ambiguous. Please use "
            + "type.plus(Duration.millis(long)) or type.minus(Duration.millis(long)) instead.",
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class JodaPlusMinusLong extends BugChecker implements MethodInvocationTreeMatcher {

  private static final ImmutableSet<String> TYPES =
      ImmutableSet.of("DateMidnight", "DateTime", "Duration", "Instant");

  private static final ImmutableSet<String> METHODS = ImmutableSet.of("plus", "minus");

  private static final Matcher<ExpressionTree> MATCHER =
      Matchers.allOf(
          buildMatcher(),
          // Allow usage by JodaTime itself
          Matchers.not(Matchers.packageStartsWith("org.joda.time")));

  private static final Matcher<ExpressionTree> DURATION_GET_MILLIS_MATCHER =
      Matchers.methodInvocation(
          MethodMatchers.instanceMethod()
              .onDescendantOf("org.joda.time.ReadableDuration")
              .named("getMillis"));

  private static Matcher<ExpressionTree> buildMatcher() {
    List<Matcher<ExpressionTree>> matchers = new ArrayList<>();
    for (String type : TYPES) {
      for (String method : METHODS) {
        matchers.add(
            Matchers.instanceMethod()
                .onExactClass("org.joda.time." + type)
                .named(method)
                .withParameters("long"));
      }
    }
    return Matchers.anyOf(matchers);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder builder = SuggestedFix.builder();
    ExpressionTree firstArgumentTree = tree.getArguments().get(0);
    String firstArgumentReplacement;
    if (DURATION_GET_MILLIS_MATCHER.matches(firstArgumentTree, state)) {
      // This is passing {@code someDuration.getMillis()} as the parameter. we can replace this
      // with {@code someDuration}.
      firstArgumentReplacement = state.getSourceForNode(ASTHelpers.getReceiver(firstArgumentTree));
    } else {
      // Wrap the long as a Duration.
      firstArgumentReplacement =
          SuggestedFixes.qualifyType(state, builder, "org.joda.time.Duration")
              + ".millis("
              + state.getSourceForNode(firstArgumentTree)
              + ")";
    }

    builder.replace(firstArgumentTree, firstArgumentReplacement);
    return describeMatch(tree, builder.build());
  }
}
