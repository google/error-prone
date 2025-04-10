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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.LiteralTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Consider using a literal of the desired type instead of casting an int literal",
    severity = WARNING)
public class IntLiteralCast extends BugChecker implements LiteralTreeMatcher {
  @Override
  public Description matchLiteral(LiteralTree tree, VisitorState state) {
    if (!tree.getKind().equals(Tree.Kind.INT_LITERAL)) {
      return NO_MATCH;
    }
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!parent.getKind().equals(Tree.Kind.TYPE_CAST)) {
      return NO_MATCH;
    }
    String source = state.getSourceForNode(tree);
    boolean decimal = !source.startsWith("0") || source.equals("0");
    String suffix =
        switch (getType(parent).getTag()) {
          case LONG -> "L";
          case FLOAT -> decimal ? ".0f" : "";
          case DOUBLE -> decimal ? ".0" : "";
          default -> "";
        };
    if (suffix.isEmpty()) {
      return NO_MATCH;
    }
    return describeMatch(
        tree,
        SuggestedFix.builder()
            .replace(getStartPosition(parent), getStartPosition(tree), "")
            .postfixWith(tree, suffix)
            .build());
  }
}
