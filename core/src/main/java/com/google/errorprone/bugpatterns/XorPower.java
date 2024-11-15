/*
 * Copyright 2019 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "The `^` operator is binary XOR, not a power operator.", severity = ERROR)
public class XorPower extends BugChecker implements BinaryTreeMatcher {
  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    if (!tree.getKind().equals(Tree.Kind.XOR)) {
      return NO_MATCH;
    }
    Tree lhsTree = tree.getLeftOperand();
    while (lhsTree instanceof BinaryTree) {
      lhsTree = ((BinaryTree) lhsTree).getRightOperand();
    }
    Number lhs = ASTHelpers.constValue(lhsTree, Number.class);
    if (lhs == null) {
      return NO_MATCH;
    }
    if (lhs.longValue() != lhs.intValue()) {
      return NO_MATCH;
    }
    switch (lhs.intValue()) {
      case 2, 10 -> {}
      default -> {
        return NO_MATCH;
      }
    }
    Number rhs = ASTHelpers.constValue(tree.getRightOperand(), Number.class);
    if (rhs == null) {
      return NO_MATCH;
    }
    if (rhs.longValue() != rhs.intValue()) {
      return NO_MATCH;
    }
    if (state.getSourceForNode(tree.getRightOperand()).startsWith("0")) {
      // hex and octal literals
      return NO_MATCH;
    }
    Description.Builder description =
        buildDescription(tree)
            .setMessage(
                String.format(
                    "The ^ operator is binary XOR, not a power operator, so '%s' will always"
                        + " evaluate to %d.",
                    state.getSourceForNode(tree), lhs.intValue() ^ rhs.intValue()));
    String suffix = lhs instanceof Long ? "L" : "";
    int start = getStartPosition(lhsTree);
    int end = state.getEndPosition(tree);
    switch (lhs.intValue()) {
      case 2 -> {
        if (rhs.intValue() <= (lhs instanceof Long ? 63 : 31)) {
          String replacement = String.format("1%s << %d", suffix, rhs);
          if (start != getStartPosition(tree)) {
            replacement = "(" + replacement + ")";
          }
          description.addFix(SuggestedFix.replace(start, end, replacement));
        }
      }
      case 10 -> {
        if (rhs.intValue() <= (lhs instanceof Long ? 18 : 9)) {
          description.addFix(
              SuggestedFix.replace(start, end, "1" + "0".repeat(rhs.intValue()) + suffix));
        }
      }
      default -> throw new AssertionError(lhs);
    }
    return description.build();
  }
}
