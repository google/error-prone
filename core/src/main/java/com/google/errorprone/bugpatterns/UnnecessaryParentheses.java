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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.STYLE;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ParenthesizedTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.Tree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "UnnecessaryParentheses",
    summary =
        "These grouping parentheses are unnecessary; it is unlikely the code will"
            + " be misinterpreted without them",
    severity = WARNING,
    tags = STYLE)
public class UnnecessaryParentheses extends BugChecker implements ParenthesizedTreeMatcher {

  @Override
  public Description matchParenthesized(ParenthesizedTree tree, VisitorState state) {
    ExpressionTree expression = tree.getExpression();
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent instanceof StatementTree) {
      return NO_MATCH;
    }else if (parent instanceof SwitchExpressionTree){
      return NO_MATCH;
    }
    if (ASTHelpers.requiresParentheses(expression, state)) {
      return NO_MATCH;
    }
    return describeMatch(
        tree,
        SuggestedFix.builder()
            .replace(getStartPosition(tree), getStartPosition(expression), "")
            .replace(state.getEndPosition(expression), state.getEndPosition(tree), "")
            .build());
  }
}
