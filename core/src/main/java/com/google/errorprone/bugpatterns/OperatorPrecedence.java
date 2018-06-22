/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.TreeInfo;
import java.util.EnumSet;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "OperatorPrecedence",
    category = JDK,
    summary = "Use grouping parenthesis to make the operator precedence explicit",
    severity = WARNING,
    tags = StandardTags.STYLE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class OperatorPrecedence extends BugChecker implements BinaryTreeMatcher {

  private static final EnumSet<Kind> CONDITIONAL =
      EnumSet.of(Kind.AND, Kind.OR, Kind.XOR, Kind.CONDITIONAL_AND, Kind.CONDITIONAL_OR);

  private static final EnumSet<Kind> SHIFT =
      EnumSet.of(Kind.LEFT_SHIFT, Kind.RIGHT_SHIFT, Kind.UNSIGNED_RIGHT_SHIFT);

  private static final EnumSet<Kind> ARITHMETIC =
      EnumSet.of(Kind.PLUS, Kind.MULTIPLY, Kind.DIVIDE, Kind.MINUS);

  private static final EnumSet<Kind> SAFE_ASSOCIATIVE_OPERATORS =
      EnumSet.of(Kind.CONDITIONAL_AND, Kind.CONDITIONAL_OR, Kind.PLUS);

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof BinaryTree)) {
      return NO_MATCH;
    }
    if (TreeInfo.opPrec(((JCBinary) tree).getTag())
        == TreeInfo.opPrec(((JCBinary) parent).getTag())) {
      return NO_MATCH;
    }
    if (!isConfusing(tree.getKind(), parent.getKind())) {
      return NO_MATCH;
    }
    return createAppropriateFix(tree, state);
  }

  private boolean isConfusing(Kind thisKind, Kind parentKind) {
    if (CONDITIONAL.contains(thisKind) && CONDITIONAL.contains(parentKind)) {
      return true;
    }
    return (SHIFT.contains(thisKind) && ARITHMETIC.contains(parentKind))
        || (SHIFT.contains(parentKind) && ARITHMETIC.contains(thisKind));
  }

  private Description createAppropriateFix(BinaryTree tree, VisitorState state) {
    // If our expression is like: (A && B) && C, replace it with (A && B && C) instead of
    // ((A && B) && C)
    return SAFE_ASSOCIATIVE_OPERATORS.contains(tree.getKind())
            && tree.getLeftOperand() instanceof ParenthesizedTree
                != tree.getRightOperand() instanceof ParenthesizedTree
            && parenthesizedChildHasKind(tree, tree.getKind())
        ? leftOrRightFix(tree, state)
        : basicFix(tree);
  }

  private Description leftOrRightFix(BinaryTree tree, VisitorState state) {
    int startPos;
    int endPos;
    String prefix = "(";
    String postfix = ")";
    if (tree.getRightOperand() instanceof ParenthesizedTree) {
      startPos = ((JCTree) tree.getRightOperand()).getStartPosition();
      endPos = ((JCTree) tree.getRightOperand()).getStartPosition() + 1;
      postfix = "";
    } else {
      startPos = state.getEndPosition(tree.getLeftOperand()) - 1;
      endPos = state.getEndPosition(tree.getLeftOperand());
      prefix = "";
    }
    return describeMatch(
        tree,
        SuggestedFix.builder()
            .prefixWith(tree, prefix)
            .replace(startPos, endPos, "")
            .postfixWith(tree, postfix)
            .build());
  }

  private Description basicFix(BinaryTree tree) {
    return describeMatch(
        tree, SuggestedFix.builder().prefixWith(tree, "(").postfixWith(tree, ")").build());
  }

  private static boolean parenthesizedChildHasKind(BinaryTree tree, Kind kind) {
    Kind childKind =
        ASTHelpers.stripParentheses(
                tree.getLeftOperand() instanceof ParenthesizedTree
                    ? tree.getLeftOperand()
                    : tree.getRightOperand())
            .getKind();
    return childKind == kind;
  }
}
