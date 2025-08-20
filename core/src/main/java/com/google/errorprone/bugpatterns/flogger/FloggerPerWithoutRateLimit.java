/*
 * Copyright 2025 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns.flogger;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Checks for usages of {@code per} without a corresponding call to {@code atMostEvery}, {@code
 * every}, or {@code onAverageEvery} in the same logging chain.
 */
@BugPattern(
    summary =
        "per() methods are no-ops unless combined with atMostEvery(), every(), or onAverageEvery()",
    severity = WARNING)
public class FloggerPerWithoutRateLimit extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> LOG_METHOD =
      instanceMethod().onDescendantOf("com.google.common.flogger.LoggingApi").named("log");

  private static final Matcher<ExpressionTree> PER_METHOD =
      instanceMethod().onDescendantOf("com.google.common.flogger.LoggingApi").named("per");

  private static final Matcher<ExpressionTree> RATE_LIMITING_METHOD =
      instanceMethod()
          .onDescendantOf("com.google.common.flogger.LoggingApi")
          .namedAnyOf("atMostEvery", "every", "onAverageEvery");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!LOG_METHOD.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    ExpressionTree perNode = null;
    for (ExpressionTree receiver = tree;
        receiver instanceof MethodInvocationTree;
        receiver = getReceiver(receiver)) {
      if (RATE_LIMITING_METHOD.matches(receiver, state)) {
        return Description.NO_MATCH;
      }
      if (PER_METHOD.matches(receiver, state)) {
        perNode = receiver;
      }
    }

    if (perNode != null) {
      return describeMatch(perNode);
    }

    return Description.NO_MATCH;
  }
}
