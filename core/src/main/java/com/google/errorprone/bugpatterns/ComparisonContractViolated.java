/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodHasArity;
import static com.google.errorprone.matchers.Matchers.methodHasVisibility;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.MethodVisibility.Visibility.PUBLIC;
import static com.google.errorprone.suppliers.Suppliers.INT_TYPE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.EnumSet;
import java.util.Set;

/** @author Louis Wasserman */
@BugPattern(
  name = "ComparisonContractViolated",
  summary = "This comparison method violates the contract",
  explanation =
      "The comparison contract states that `sgn(compare(x, y)) == -sgn(compare(y, x))`."
          + " (An immediate corollary is that `compare(x, x) == 0`.)  This comparison "
          + "implementation either a) cannot return 0, b) cannot return a negative value but may "
          + "return a positive value, or c) cannot return a positive value but may return a "
          + "negative value.\n\n"
          + "The results of violating this contract can include `TreeSet.contains` never"
          + " returning true or `Collections.sort` failing with an IllegalArgumentException"
          + " arbitrarily."
  ,
  severity = SeverityLevel.ERROR,
  category = Category.JDK,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class ComparisonContractViolated extends BugChecker implements MethodTreeMatcher {
  /** Matcher for the overriding method of 'int java.lang.Comparable.compareTo(T other)' */
  private static final Matcher<MethodTree> COMPARABLE_METHOD_MATCHER =
      allOf(
          methodIsNamed("compareTo"),
          methodHasVisibility(PUBLIC),
          methodReturns(INT_TYPE),
          methodHasArity(1));

  private static final Matcher<ClassTree> COMPARABLE_CLASS_MATCHER =
      isSubtypeOf("java.lang.Comparable");

  /** Matcher for the overriding method of 'int java.util.Comparator.compare(T o1, T o2)' */
  private static final Matcher<MethodTree> COMPARATOR_METHOD_MATCHER =
      allOf(
          methodIsNamed("compare"),
          methodHasVisibility(PUBLIC),
          methodReturns(INT_TYPE),
          methodHasArity(2));

  private static final Matcher<ClassTree> COMPARATOR_CLASS_MATCHER =
      isSubtypeOf("java.util.Comparator");

  private enum ComparisonResult {
    NEGATIVE_CONSTANT,
    ZERO,
    POSITIVE_CONSTANT,
    NONCONSTANT;
  }

  private static final TreeVisitor<ComparisonResult, VisitorState> CONSTANT_VISITOR =
      new SimpleTreeVisitor<ComparisonResult, VisitorState>(ComparisonResult.NONCONSTANT) {
        private ComparisonResult forInt(int x) {
          if (x < 0) {
            return ComparisonResult.NEGATIVE_CONSTANT;
          } else if (x > 0) {
            return ComparisonResult.POSITIVE_CONSTANT;
          } else {
            return ComparisonResult.ZERO;
          }
        }

        @Override
        public ComparisonResult visitMemberSelect(MemberSelectTree node, VisitorState state) {
          Symbol sym = ASTHelpers.getSymbol(node);
          if (sym instanceof VarSymbol) {
            Object value = ((VarSymbol) sym).getConstantValue();
            if (value instanceof Integer) {
              return forInt((Integer) value);
            }
          }
          return super.visitMemberSelect(node, state);
        }

        @Override
        public ComparisonResult visitIdentifier(IdentifierTree node, VisitorState state) {
          Symbol sym = ASTHelpers.getSymbol(node);
          if (sym instanceof VarSymbol) {
            Object value = ((VarSymbol) sym).getConstantValue();
            if (value instanceof Integer) {
              return forInt((Integer) value);
            }
          }
          return super.visitIdentifier(node, state);
        }

        @Override
        public ComparisonResult visitLiteral(LiteralTree node, VisitorState state) {
          if (node.getValue() instanceof Integer) {
            return forInt((Integer) node.getValue());
          }
          return super.visitLiteral(node, state);
        }
      };

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (tree.getBody() == null) {
      return Description.NO_MATCH;
    }
    // Test that the match is in a Comparable.compareTo or Comparator.compare method.
    ClassTree declaringClass = ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
    if (!COMPARABLE_CLASS_MATCHER.matches(declaringClass, state)
        && !COMPARATOR_CLASS_MATCHER.matches(declaringClass, state)) {
      return Description.NO_MATCH;
    }
    if (!COMPARABLE_METHOD_MATCHER.matches(tree, state)
        && !COMPARATOR_METHOD_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    final Set<ComparisonResult> seenResults = EnumSet.noneOf(ComparisonResult.class);

    final TreeVisitor<Void, VisitorState> visitReturnExpression =
        new SimpleTreeVisitor<Void, VisitorState>() {

          @Override
          protected Void defaultAction(Tree node, VisitorState state) {
            seenResults.add(node.accept(CONSTANT_VISITOR, state));
            return null;
          }

          @Override
          public Void visitConditionalExpression(
              ConditionalExpressionTree node, VisitorState state) {
            node.getTrueExpression().accept(this, state);
            node.getFalseExpression().accept(this, state);
            return null;
          }
        };

    tree.getBody()
        .accept(
            new TreeScanner<Void, VisitorState>() {
              @Override
              public Void visitReturn(ReturnTree node, VisitorState state) {
                return node.getExpression().accept(visitReturnExpression, state);
              }
            },
            state);

    if (seenResults.isEmpty() || seenResults.contains(ComparisonResult.NONCONSTANT)) {
      return Description.NO_MATCH;
    }
    if (!seenResults.contains(ComparisonResult.ZERO)) {
      if (tree.getBody().getStatements().size() == 1
          && tree.getBody().getStatements().get(0).getKind() == Kind.RETURN) {
        ReturnTree returnTree = (ReturnTree) tree.getBody().getStatements().get(0);
        if (returnTree.getExpression().getKind() == Kind.CONDITIONAL_EXPRESSION) {
          ConditionalExpressionTree condTree =
              (ConditionalExpressionTree) returnTree.getExpression();
          ExpressionTree conditionExpr = condTree.getCondition();
          conditionExpr = ASTHelpers.stripParentheses(conditionExpr);
          if (!(conditionExpr instanceof BinaryTree)) {
            return describeMatch(tree);
          }
          ComparisonResult trueConst = condTree.getTrueExpression().accept(CONSTANT_VISITOR, state);
          ComparisonResult falseConst =
              condTree.getFalseExpression().accept(CONSTANT_VISITOR, state);
          boolean trueFirst;
          if (trueConst == ComparisonResult.NEGATIVE_CONSTANT
              && falseConst == ComparisonResult.POSITIVE_CONSTANT) {
            trueFirst = true;
          } else if (trueConst == ComparisonResult.POSITIVE_CONSTANT
              && falseConst == ComparisonResult.NEGATIVE_CONSTANT) {
            trueFirst = false;
          } else {
            return describeMatch(tree);
          }
          switch (conditionExpr.getKind()) {
            case LESS_THAN:
            case LESS_THAN_EQUAL:
              break;
            case GREATER_THAN:
            case GREATER_THAN_EQUAL:
              trueFirst = !trueFirst;
              break;
            default:
              return describeMatch(tree);
          }
          BinaryTree binaryExpr = (BinaryTree) conditionExpr;
          Type ty = ASTHelpers.getType(binaryExpr.getLeftOperand());
          Types types = Types.instance(state.context);
          Symtab symtab = Symtab.instance(state.context);

          ExpressionTree first =
              trueFirst ? binaryExpr.getLeftOperand() : binaryExpr.getRightOperand();
          ExpressionTree second =
              trueFirst ? binaryExpr.getRightOperand() : binaryExpr.getLeftOperand();

          String compareType;
          if (types.isSameType(ty, symtab.intType)) {
            compareType = "Integer";
          } else if (types.isSameType(ty, symtab.longType)) {
            compareType = "Long";
          } else {
            return describeMatch(tree);
          }
          return describeMatch(
              condTree,
              SuggestedFix.replace(
                  condTree,
                  String.format(
                      "%s.compare(%s, %s)",
                      compareType, state.getSourceForNode(first), state.getSourceForNode(second))));
        }
      }

      return describeMatch(tree);
    }
    if (COMPARATOR_METHOD_MATCHER.matches(tree, state)
        && (seenResults.contains(ComparisonResult.NEGATIVE_CONSTANT)
            != seenResults.contains(ComparisonResult.POSITIVE_CONSTANT))) {
      // note that a Comparable.compareTo implementation can be asymmetric!
      // See e.g. com.google.common.collect.Cut.BelowAll.
      return describeMatch(tree);
    } else {
      return Description.NO_MATCH;
    }
  }
}
