/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "ReachabilityFenceUsage",
  summary = "reachabilityFence should always be called inside a finally block",
  severity = WARNING
)
public class ReachabilityFenceUsage extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> FENCE_MATCHER =
      staticMethod().onClass("java.lang.ref.Reference").named("reachabilityFence");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!FENCE_MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    Tree previous = null;
    OUTER:
    for (Tree enclosing : state.getPath().getParentPath()) {
      switch (enclosing.getKind()) {
        case TRY:
          if (((TryTree) enclosing).getFinallyBlock().equals(previous)) {
            return NO_MATCH;
          }
          break;
        case CLASS:
        case METHOD:
        case LAMBDA_EXPRESSION:
          break OUTER;
        default: // fall out
      }
      previous = enclosing;
    }
    return describeMatch(tree);
  }
}
