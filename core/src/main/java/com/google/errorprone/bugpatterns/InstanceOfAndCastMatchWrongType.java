/*
 * Copyright 2012 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TypeCastTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Types;
import org.jspecify.annotations.Nullable;

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
@BugPattern(
    summary = "Casting inside an if block should be plausibly consistent with the instanceof type",
    severity = WARNING)
public class InstanceOfAndCastMatchWrongType extends BugChecker implements TypeCastTreeMatcher {

  @Override
  public Description matchTypeCast(TypeCastTree typeCastTree, VisitorState visitorState) {
    // finds path from first enclosing node to the top level tree
    TreePath pathToTop =
        ASTHelpers.findPathFromEnclosingNodeToTopLevel(visitorState.getPath(), IfTree.class);
    while (pathToTop != null) {
      IfTree ifTree = (IfTree) pathToTop.getLeaf();

      ExpressionTree expressionTree = ASTHelpers.stripParentheses(ifTree.getCondition());
      TreeScannerInstanceOfWrongType treeScannerInstanceOfWrongType =
          new TreeScannerInstanceOfWrongType(visitorState);
      treeScannerInstanceOfWrongType.scan(expressionTree, typeCastTree.getExpression());

      InstanceOfTree instanceOfTree = treeScannerInstanceOfWrongType.getRelevantTree();

      // check to make sure that the if tree encountered has a relevant instanceof statement
      // in the condition
      if (instanceOfTree == null) {
        pathToTop = ASTHelpers.findPathFromEnclosingNodeToTopLevel(pathToTop, IfTree.class);
        continue;
      }

      // if the specific TypeCastTree is in the else statement, then ignore
      if (ifTree.getElseStatement() != null
          && Iterables.contains(visitorState.getPath(), ifTree.getElseStatement())) {
        pathToTop = ASTHelpers.findPathFromEnclosingNodeToTopLevel(pathToTop, IfTree.class);
        continue;
      }

      Types types = visitorState.getTypes();

      // Scan for earliest assignment expression in "then" block of if statement
      treeScannerInstanceOfWrongType.scan(
          ifTree.getThenStatement(), instanceOfTree.getExpression());
      int pos = treeScannerInstanceOfWrongType.earliestStart;
      if (pos < getStartPosition(typeCastTree)) {
        pathToTop = ASTHelpers.findPathFromEnclosingNodeToTopLevel(pathToTop, IfTree.class);
        continue;
      }

      var targetType =
          getType(
              instanceOfTree.getType() == null
                  ? instanceOfTree.getPattern()
                  : instanceOfTree.getType());

      boolean isCastable =
          types.isCastable(types.erasure(targetType), types.erasure(getType(typeCastTree)));

      ExpressionTree typeCastExp = typeCastTree.getExpression();
      boolean isSameExpression = expressionsEqual(typeCastExp, instanceOfTree.getExpression());
      if (isSameExpression) {
        if (!isCastable) {
          return describeMatch(typeCastTree);
        } else {
          return Description.NO_MATCH;
        }
      }
      pathToTop = ASTHelpers.findPathFromEnclosingNodeToTopLevel(pathToTop, IfTree.class);
    }
    return Description.NO_MATCH;
  }

  static class TreeScannerInstanceOfWrongType extends TreeScanner<Void, ExpressionTree> {

    // represents the earliest position of a relevant assignment
    int earliestStart = Integer.MAX_VALUE;
    private InstanceOfTree relevantTree;
    private boolean notApplicable = false;
    private final VisitorState state;

    @Nullable InstanceOfTree getRelevantTree() {
      if (notApplicable) {
        return null;
      }
      return relevantTree;
    }

    public TreeScannerInstanceOfWrongType(VisitorState currState) {
      state = currState;
    }

    @Override
    public Void visitBinary(BinaryTree binTree, ExpressionTree expr) {
      if (binTree.getKind().equals(Kind.CONDITIONAL_OR)) {
        notApplicable = true;
      }
      return super.visitBinary(binTree, expr);
    }

    @Override
    public Void visitUnary(UnaryTree tree, ExpressionTree expr) {
      if (tree.getKind().equals(Kind.LOGICAL_COMPLEMENT)) {
        notApplicable = true;
      }
      return super.visitUnary(tree, expr);
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree tree, ExpressionTree expr) {
      if (expressionsEqual(tree.getExpression(), expr)) {
        relevantTree = tree;
      }
      return super.visitInstanceOf(tree, expr);
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, ExpressionTree expr) {
      if (expressionsEqual(tree.getVariable(), expr)) {
        earliestStart = Math.min(earliestStart, state.getEndPosition(tree));
      }
      return super.visitAssignment(tree, expr);
    }
  }

  /**
   * Determines whether two {@link ExpressionTree} instances are equal. Only handles the cases
   * relevant to this checker: array accesses, identifiers, and literals. Returns false for all
   * other cases.
   */
  private static boolean expressionsEqual(ExpressionTree expr1, ExpressionTree expr2) {
    if (expr1 instanceof ArrayAccessTree arrayAccessTree1
        && expr2 instanceof ArrayAccessTree arrayAccessTree2) {
      return expressionsEqual(arrayAccessTree1.getExpression(), arrayAccessTree2.getExpression())
          && expressionsEqual(arrayAccessTree1.getIndex(), arrayAccessTree2.getIndex());
    }

    if (expr1 instanceof LiteralTree literalTree1 && expr2 instanceof LiteralTree literalTree2) {
      return literalTree1.getValue().equals(literalTree2.getValue());
    }

    if (expr1 instanceof IdentifierTree identifierTree1
        && expr2 instanceof IdentifierTree identifierTree2) {
      return getSymbol(identifierTree1).equals(getSymbol(identifierTree2));
    }

    return false;
  }
}
