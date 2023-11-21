/*
 * Copyright 2023 The Error Prone Authors.
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
import static com.google.errorprone.fixes.SuggestedFixes.renameMethodInvocation;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;

/** A BugPattern; see the summary. */
@BugPattern(
    summary =
        "Iterating over `Multimap.keys()` does not collapse duplicates. Did you mean `keySet()`?",
    severity = WARNING)
public final class MultimapKeys extends BugChecker implements MethodInvocationTreeMatcher {
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return KEYS.matches(tree, state) && isBeingIteratedOver(state)
        ? describeMatch(tree, renameMethodInvocation(tree, "keySet", state))
        : NO_MATCH;
  }

  private static boolean isBeingIteratedOver(VisitorState state) {
    var parent = state.getPath().getParentPath().getLeaf();
    if (parent instanceof EnhancedForLoopTree) {
      return true;
    }
    if (parent instanceof MemberSelectTree) {
      var grandparent = state.getPath().getParentPath().getParentPath().getLeaf();
      return grandparent instanceof MethodInvocationTree
          && FOR_EACH.matches((ExpressionTree) grandparent, state);
    }
    return false;
  }

  private static final Matcher<ExpressionTree> KEYS =
      instanceMethod().onDescendantOf("com.google.common.collect.Multimap").named("keys");

  private static final Matcher<ExpressionTree> FOR_EACH =
      instanceMethod().onDescendantOf("java.util.Collection").named("forEach");
}
