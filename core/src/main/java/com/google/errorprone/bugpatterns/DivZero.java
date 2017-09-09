/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.kindIs;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CompoundAssignmentTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * Matches the behaviour of javac's divzero xlint warning.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
  name = "DivZero",
  altNames = "divzero",
  summary = "Division by integer literal zero",
  explanation = "This code will cause a runtime arithmetic exception if it is executed.",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class DivZero extends BugChecker
    implements BinaryTreeMatcher, CompoundAssignmentTreeMatcher {

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    return matchDivZero(tree, tree.getRightOperand(), state);
  }

  @Override
  public Description matchCompoundAssignment(CompoundAssignmentTree tree, VisitorState state) {
    return matchDivZero(tree, tree.getExpression(), state);
  }

  private Description matchDivZero(Tree tree, ExpressionTree operand, VisitorState state) {
    if (!anyOf(kindIs(Kind.DIVIDE), kindIs(Kind.DIVIDE_ASSIGNMENT)).matches(tree, state)) {
      return Description.NO_MATCH;
    }

    if (!kindIs(Kind.INT_LITERAL).matches(operand, state)) {
      return Description.NO_MATCH;
    }

    LiteralTree rightOperand = (LiteralTree) operand;
    if (((Integer) rightOperand.getValue()) != 0) {
      return Description.NO_MATCH;
    }

    // Find and replace enclosing Statement.
    StatementTree enclosingStmt =
        ASTHelpers.findEnclosingNode(state.getPath(), StatementTree.class);
    return (enclosingStmt != null)
        ? describeMatch(
            tree,
            SuggestedFix.replace(enclosingStmt, "throw new ArithmeticException(\"/ by zero\");"))
        : describeMatch(tree);
  }
}
