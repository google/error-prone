/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TypeCastTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import javax.annotation.Nullable;

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
@BugPattern(
  name = "InstanceOfAndCastMatchWrongType",
  summary = "Casting inside an if block should be plausibly consistent with the instanceof type",
  category = JDK,
  severity = WARNING
)
public class InstanceOfAndCastMatchWrongType extends BugChecker implements TypeCastTreeMatcher {

  @Override
  public Description matchTypeCast(TypeCastTree typeCastTree, VisitorState visitorState) {
    CastingMatcher castingMatcher = new CastingMatcher();

    if (!(typeCastTree.getExpression() instanceof IdentifierTree
        || typeCastTree.getExpression() instanceof ArrayAccessTree)) {
      return Description.NO_MATCH;
    }
    if (castingMatcher.matches(typeCastTree, visitorState)) {
      return buildDescription(typeCastTree)
          .addFix(
              SuggestedFix.replace(castingMatcher.nodeToReplace, typeCastTree.getType().toString()))
          .build();
    }
    return Description.NO_MATCH;
  }

  /** Matches any Tree that casts to a type that is disjoint from the stored type. */
  private static class CastingMatcher implements Matcher<Tree> {

    Tree nodeToReplace;

    /**
     * This method matches a TypeCastTree, then iterates up to the nearest if tree with a condition
     * that looks for an instanceof pattern that matches the expression acted upon. It then checks
     * to see if the cast is plausible (if the classes are not disjoint)
     *
     * @param tree TypeCastTree that is matched
     * @param state VisitorState
     */
    @Override
    public boolean matches(Tree tree, VisitorState state) {

      // finds path from first enclosing node to the top level tree
      TreePath pathToTop =
          ASTHelpers.findPathFromEnclosingNodeToTopLevel(state.getPath(), IfTree.class);
      while (pathToTop != null) {

        IfTree ifTree = (IfTree) pathToTop.getLeaf();

        ExpressionTree expressionTree = ASTHelpers.stripParentheses(ifTree.getCondition());
        TreeScannerInstanceOfWrongType treeScannerInstanceOfWrongType =
            new TreeScannerInstanceOfWrongType(state);
        treeScannerInstanceOfWrongType.scan(expressionTree, ((TypeCastTree) tree).getExpression());

        Tree treeInstance = treeScannerInstanceOfWrongType.getRelevantTree();

        // check to make sure that the if tree encountered has a relevant instanceof statement
        // in the condition
        if (treeInstance == null) {
          pathToTop = ASTHelpers.findPathFromEnclosingNodeToTopLevel(pathToTop, IfTree.class);
          continue;
        }

        // if the specific TypeCastTree is in the else statement, then ignore
        if (ifTree.getElseStatement() != null
            && Iterables.contains(state.getPath(), ifTree.getElseStatement())) {
          return false;
        }

        Types types = state.getTypes();
        InstanceOfTree instanceOfTree = (InstanceOfTree) treeInstance;
        nodeToReplace = instanceOfTree.getType();

        // Scan for earliest assignment expression in "then" block of if statement
        treeScannerInstanceOfWrongType.scan(
            ifTree.getThenStatement(), instanceOfTree.getExpression());
        int pos = treeScannerInstanceOfWrongType.earliestStart;
        if (pos < ((JCTree) tree).getStartPosition()) {
          return false;
        }

        boolean isCastable =
            types.isCastable(
                types.erasure(ASTHelpers.getType(instanceOfTree.getType())),
                types.erasure(ASTHelpers.getType(tree)));

        ExpressionTree typeCastExp = ((TypeCastTree) tree).getExpression();
        boolean isSameExpression = expressionsEqual(typeCastExp, instanceOfTree.getExpression());
        return isSameExpression && !isCastable;
      }
      return false;
    }
  }

  static class TreeScannerInstanceOfWrongType extends TreeScanner<Void, ExpressionTree> {

    // represents the earliest position of a relevant assignment
    int earliestStart = Integer.MAX_VALUE;
    private InstanceOfTree relevantTree;
    private boolean notApplicable = false;
    private final VisitorState state;

    @Nullable
    InstanceOfTree getRelevantTree() {
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
    if (!expr1.getKind().equals(expr2.getKind())) {
      return false;
    }

    if (!expr1.getKind().equals(Kind.ARRAY_ACCESS)
        && !expr1.getKind().equals(Kind.IDENTIFIER)
        && !(expr1 instanceof LiteralTree)) {
      return false;
    }

    if (expr1.getKind() == Kind.ARRAY_ACCESS) {
      ArrayAccessTree arrayAccessTree1 = (ArrayAccessTree) expr1;
      ArrayAccessTree arrayAccessTree2 = (ArrayAccessTree) expr2;
      return expressionsEqual(arrayAccessTree1.getExpression(), arrayAccessTree2.getExpression())
          && expressionsEqual(arrayAccessTree1.getIndex(), arrayAccessTree2.getIndex());
    }

    if (expr1 instanceof LiteralTree) {
      LiteralTree literalTree1 = (LiteralTree) expr1;
      LiteralTree literalTree2 = (LiteralTree) expr2;
      return literalTree1.getValue().equals(literalTree2.getValue());
    }

    return Objects.equal(ASTHelpers.getSymbol(expr1), ASTHelpers.getSymbol(expr2));
  }
}
