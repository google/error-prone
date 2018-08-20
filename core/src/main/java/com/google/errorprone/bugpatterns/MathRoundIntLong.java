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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers.MethodNameMatcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Check for calls to Math's {@link Math#round} with an integer or long parameter.
 *
 * @author seibelsabrina@google.com (Sabrina Seibel)
 */
@BugPattern(
    name = "MathRoundIntLong",
    summary = "Math.round(Integer) results in truncation",
    explanation =
        "Math.round() called with an integer or long type results in truncation"
            + " because Math.round only accepts floats or doubles and some integers and longs can't"
            + " be represented with float.",
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class MathRoundIntLong extends BugChecker implements MethodInvocationTreeMatcher {
  private static final MethodNameMatcher MATH_ROUND_CALLS =
      staticMethod().onClass("java.lang.Math").named("round");

  private static final Matcher<MethodInvocationTree> ROUND_CALLS_WITH_INT_ARG =
      allOf(
          MATH_ROUND_CALLS, argument(0, anyOf(isSameType("int"), isSameType("java.lang.Integer"))));

  private static final Matcher<MethodInvocationTree> ROUND_CALLS_WITH_LONG_ARG =
      allOf(MATH_ROUND_CALLS, argument(0, anyOf(isSameType("long"), isSameType("java.lang.Long"))));

  private static final Matcher<MethodInvocationTree> ROUND_CALLS_WITH_INT_OR_LONG_ARG =
      anyOf(ROUND_CALLS_WITH_INT_ARG, ROUND_CALLS_WITH_LONG_ARG);

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return ROUND_CALLS_WITH_INT_OR_LONG_ARG.matches(tree, state)
        ? removeMathRoundCall(tree, state)
        : Description.NO_MATCH;
  }

  private Description removeMathRoundCall(MethodInvocationTree tree, VisitorState state) {
    if (ROUND_CALLS_WITH_INT_ARG.matches(tree, state)) {
      if (ASTHelpers.requiresParentheses(Iterables.getOnlyElement(tree.getArguments()), state)) {
        return buildDescription(tree)
            .addFix(
                SuggestedFix.builder()
                    .prefixWith(tree, "(")
                    .replace(tree, state.getSourceForNode(tree.getArguments().get(0)))
                    .postfixWith(tree, ")")
                    .build())
            .build();
      }
      return describeMatch(
          tree, SuggestedFix.replace(tree, state.getSourceForNode(tree.getArguments().get(0))));
    } else if (ROUND_CALLS_WITH_LONG_ARG.matches(tree, state)) {
      // TODO(b/112270644): skip Ints.saturatedCast fix if guava isn't on the classpath
      return describeMatch(
          tree,
          SuggestedFix.builder()
              .addImport("com.google.common.primitives.Ints")
              .prefixWith(tree, "Ints.saturatedCast(")
              .replace(tree, state.getSourceForNode(tree.getArguments().get(0)))
              .postfixWith(tree, ")")
              .build());
    }
    throw new AssertionError("Unknown argument type to round call: " + tree);
  }
}
