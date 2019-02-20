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
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.packageStartsWith;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;

/** Check for calls to {@code duration.withNanos(int)}. */
@BugPattern(
    name = "JavaDurationWithNanos",
    summary = "Use of java.time.Duration.withNanos(int) is not allowed.",
    explanation =
        "Duration's withNanos(int) method is often a source of bugs because it returns a copy of "
            + "the current Duration instance, but _only_ the nano field is mutated (the seconds "
            + "field is copied directly). Use Duration.ofSeconds(duration.getSeconds(), nanos) "
            + "instead.",
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class JavaDurationWithNanos extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      allOf(
          Matchers.instanceMethod()
              .onExactClass("java.time.Duration")
              .named("withNanos")
              .withParameters("int"),
          // Allow usage by java.time itself
          not(packageStartsWith("java.time")));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder builder = SuggestedFix.builder();
    ExpressionTree nanosArg = Iterables.getOnlyElement(tree.getArguments());
    ExpressionTree receiver = ASTHelpers.getReceiver(tree);
    String replacement =
        SuggestedFixes.qualifyType(state, builder, "java.time.Duration")
            + ".ofSeconds("
            + state.getSourceForNode(receiver)
            + ".getSeconds(), ";

    builder.replace(
        ((JCTree) tree).getStartPosition(), ((JCTree) nanosArg).getStartPosition(), replacement);
    return describeMatch(tree, builder.build());
  }
}
