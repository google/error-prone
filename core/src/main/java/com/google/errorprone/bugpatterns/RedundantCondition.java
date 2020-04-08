/*
 * Copyright 2020 The Error Prone Authors.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ConditionalExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ElementKind;

/**
 * Checks if condition or assignment is always true.
 *
 * @author abhatiya@google.com (Ankush Bhatiya)
 */
// TODO: Use Checker Framework Dataflow Library for catching more issues.
@BugPattern(
    name = "RedundantCondition",
    summary = "Redundant usage of a boolean variable with known value",
    severity = WARNING)
public class RedundantCondition extends BugChecker
    implements IfTreeMatcher,
        AssignmentTreeMatcher,
        ConditionalExpressionTreeMatcher,
        VariableTreeMatcher {

  private static final ImmutableList<RedundantExpressionMatcher> REDUNDANT_EXPRESSION_MATCHERS =
      ImmutableList.of(new RedundantVariableMatcher());

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    if (tree.getExpression() instanceof BinaryTree) {
      return matchExpression(tree.getExpression(), state);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (tree.getInitializer() instanceof BinaryTree) {
      return matchExpression(tree.getInitializer(), state);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchConditionalExpression(
      ConditionalExpressionTree tree, VisitorState state) {
    return matchExpression(tree.getCondition(), state);
  }

  @Override
  public Description matchIf(IfTree ifTree, VisitorState state) {
    return matchExpression(ifTree.getCondition(), state);
  }

  @SuppressWarnings("TreeToString")
  private Description matchExpression(ExpressionTree expressionTree, VisitorState state) {
    TreePath childPath = state.getPath();
    TreePath parentPath = childPath.getParentPath();
    IfTree ifTree = null;
    // Find a IfTree enclosing this expression.
    while (parentPath != null) {
      if (parentPath.getLeaf() instanceof IfTree) {
        IfTree enclosingIfTree = (IfTree) parentPath.getLeaf();
        // Only match if expressionTree is in ThenStatement.
        if (enclosingIfTree.getThenStatement().equals(childPath.getLeaf())) {
          ifTree = enclosingIfTree;
        }
      }
      childPath = parentPath;
      parentPath = parentPath.getParentPath();
    }

    if (ifTree == null) {
      return Description.NO_MATCH;
    }
    List<String> matchedExpressions = new ArrayList<>();
    for (RedundantExpressionMatcher matcher : REDUNDANT_EXPRESSION_MATCHERS) {
      List<ExpressionTree> expressionToCheck = matcher.extractExpressionsToCheck(expressionTree);
      List<ExpressionTree> expressionToCheckAgainst =
          matcher.extractExpressionsToCheckAgainst(
              ifTree.getCondition(), ifTree.getThenStatement());
      for (ExpressionTree lhs : expressionToCheck) {
        for (ExpressionTree rhs : expressionToCheckAgainst) {
          if (matcher.isSame(lhs, rhs)) {
            matchedExpressions.add(lhs.toString());
          }
        }
      }
    }

    if (!matchedExpressions.isEmpty()) {
      return buildDescription(expressionTree)
          .setMessage(
              "Redundant usage of a boolean expression "
                  + matchedExpressions
                  + " that is known to be `true`")
          .build();
    }

    return Description.NO_MATCH;
  }

  /** */
  private interface RedundantExpressionMatcher {

    /** List of expressions that needs to be checked. */
    List<ExpressionTree> extractExpressionsToCheck(ExpressionTree expressionTree);

    /**
     * List of expressions that are in parent if block.
     *
     * @param ifCondition Parent if block condition expression
     * @param thenStatement Then statement of parent if block, useful in identifying variables that
     *     are re-assigned.
     */
    List<ExpressionTree> extractExpressionsToCheckAgainst(
        ExpressionTree ifCondition, StatementTree thenStatement);

    boolean isSame(ExpressionTree lhs, ExpressionTree rhs);
  }

  /** Extracts redundant variables (identifiers) from the Expression. */
  private static class RedundantVariableMatcher implements RedundantExpressionMatcher {

    @Override
    public List<ExpressionTree> extractExpressionsToCheck(ExpressionTree expressionTree) {
      ExpressionTree strippedParentheses = ASTHelpers.stripParentheses(expressionTree);
      List<ExpressionTree> extractedVariables = new ArrayList<>();
      extractVariables(strippedParentheses, extractedVariables, false, ImmutableSet.of());
      return ImmutableList.copyOf(extractedVariables);
    }

    @Override
    public List<ExpressionTree> extractExpressionsToCheckAgainst(
        ExpressionTree ifCondition, StatementTree thenStatement) {
      ExpressionTree strippedParentheses = ASTHelpers.stripParentheses(ifCondition);
      List<ExpressionTree> extractedVariables = new ArrayList<>();
      AssignedSymbolsScanner assignedSymbolsScanner = new AssignedSymbolsScanner();
      assignedSymbolsScanner.scan(thenStatement, Boolean.FALSE);
      extractVariables(
          strippedParentheses,
          extractedVariables,
          true,
          assignedSymbolsScanner.getAssignedSymbols());
      return extractedVariables;
    }

    @Override
    public boolean isSame(ExpressionTree lhs, ExpressionTree rhs) {
      if (lhs instanceof JCIdent && rhs instanceof JCIdent) {
        return ((JCIdent) lhs).sym == ((JCIdent) rhs).sym;
      } else if (lhs instanceof JCUnary && rhs instanceof JCUnary) {
        ExpressionTree lhsUnary = ((JCUnary) lhs).getExpression();
        ExpressionTree rhsUnary = ((JCUnary) rhs).getExpression();
        if (lhsUnary instanceof JCIdent && rhsUnary instanceof JCIdent) {
          return ((JCIdent) lhsUnary).sym == ((JCIdent) rhsUnary).sym;
        }
      }
      return false;
    }

    private static void extractVariables(
        ExpressionTree expressionTree,
        List<ExpressionTree> extractedVariables,
        boolean conditionalAndToMatch,
        Set<Symbol> symbolsToIgnore) {
      if (expressionTree instanceof BinaryTree) {
        BinaryTree binaryTree = (BinaryTree) expressionTree;
        if (conditionalAndToMatch && (binaryTree.getKind() != Kind.CONDITIONAL_AND)) {
          return;
        }
        extractVariables(
            binaryTree.getLeftOperand(),
            extractedVariables,
            conditionalAndToMatch,
            symbolsToIgnore);
        extractVariables(
            binaryTree.getRightOperand(),
            extractedVariables,
            conditionalAndToMatch,
            symbolsToIgnore);
      } else if (expressionTree instanceof JCIdent) {
        if (shouldAddSymbol((JCIdent) expressionTree, symbolsToIgnore)) {
          extractedVariables.add(expressionTree);
        }
      } else if (expressionTree instanceof JCUnary) {
        JCUnary jcUnary = (JCUnary) expressionTree;
        if (jcUnary.getKind() == Kind.LOGICAL_COMPLEMENT
            && jcUnary.getExpression() instanceof JCIdent) {
          if (shouldAddSymbol((JCIdent) jcUnary.getExpression(), symbolsToIgnore)) {
            extractedVariables.add(expressionTree);
          }
        }
      }
    }

    private static boolean shouldAddSymbol(JCIdent jcIdent, Set<Symbol> symbolsToIgnore) {
      Symbol symbol = jcIdent.sym;
      if (symbol.getKind() != ElementKind.LOCAL_VARIABLE
          && symbol.getKind() != ElementKind.PARAMETER) {
        return false;
      }
      return !symbolsToIgnore.contains(symbol);
    }
  }

  private static class AssignedSymbolsScanner extends TreeScanner<Void, Boolean> {

    final Set<Symbol> assignedSymbols;

    AssignedSymbolsScanner() {
      this.assignedSymbols = new HashSet<>();
    }

    @Override
    public Void visitAssignment(AssignmentTree assignmentTree, Boolean unused) {
      ExpressionTree variable = assignmentTree.getVariable();
      if (variable instanceof JCIdent) {
        JCIdent identifierTree = (JCIdent) variable;
        assignedSymbols.add(identifierTree.sym);
      }
      return super.visitAssignment(assignmentTree, unused);
    }

    Set<Symbol> getAssignedSymbols() {
      return ImmutableSet.copyOf(assignedSymbols);
    }
  }
}
