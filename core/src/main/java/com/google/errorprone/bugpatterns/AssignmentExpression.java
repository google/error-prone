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

import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.VarSymbol;

/** A BugPattern; see the summary. */
@BugPattern(
    summary =
        "The use of an assignment expression can be surprising and hard to read; consider factoring"
            + " out the assignment to a separate statement.",
    severity = SeverityLevel.WARNING)
public final class AssignmentExpression extends BugChecker implements AssignmentTreeMatcher {

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent instanceof ExpressionStatementTree) {
      return Description.NO_MATCH;
    }
    if (parent instanceof AnnotationTree) {
      return Description.NO_MATCH;
    }
    if (parent instanceof LambdaExpressionTree) {
      return Description.NO_MATCH;
    }
    // Exempt the C-ism of (foo = getFoo()) != null, etc.
    if (parent instanceof ParenthesizedTree
        && state.getPath().getParentPath().getParentPath().getLeaf() instanceof BinaryTree) {
      return Description.NO_MATCH;
    }
    // Detect duplicate assignments: a = a = foo() so that we can generate a fix.
    if (isDuplicateAssignment(tree, parent)) {
      return describeMatch(
          tree, SuggestedFix.replace(tree, state.getSourceForNode(tree.getExpression())));
    }
    // If we got here it's something like x = y = 0, which is odd but not disallowed.
    if (parent instanceof AssignmentTree) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree);
  }

  private static boolean isDuplicateAssignment(AssignmentTree tree, Tree parent) {
    if (!(tree.getVariable() instanceof IdentifierTree
        && getSymbol(tree.getVariable()) instanceof VarSymbol varSymbol)) {
      return false;
    }
    return switch (parent.getKind()) {
      case ASSIGNMENT ->
          ((AssignmentTree) parent).getVariable() instanceof IdentifierTree identifierTree
              && varSymbol.equals(getSymbol(identifierTree));
      case VARIABLE -> varSymbol.equals(getSymbol((VariableTree) parent));
      default -> false;
    };
  }
}
