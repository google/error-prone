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

import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.STYLE;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ParenthesizedTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.StatementTree;
import com.sun.tools.javac.tree.JCTree;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "UnnecessaryParentheses",
    summary = "Unnecessary use of grouping parentheses",
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION,
    tags = STYLE)
public class UnnecessaryParentheses extends BugChecker implements ParenthesizedTreeMatcher {

  @Override
  public Description matchParenthesized(ParenthesizedTree tree, VisitorState state) {
    ExpressionTree expression = tree.getExpression();
    if (state.getPath().getParentPath().getLeaf() instanceof StatementTree) {
      return NO_MATCH;
    }
    if (ASTHelpers.requiresParentheses(expression, state)) {
      return NO_MATCH;
    }
    return describeMatch(
        tree,
        SuggestedFix.builder()
            .replace(
                ((JCTree) tree).getStartPosition(), ((JCTree) expression).getStartPosition(), "")
            .replace(state.getEndPosition(expression), state.getEndPosition(tree), "")
            .build());
  }
}
