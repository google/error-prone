/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.isArrayType;
import static com.google.errorprone.matchers.Matchers.isDescendantOfMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodSelect;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.EnhancedForLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.WhileLoopTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.WhileLoopTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;

/**
 * @author amshali@google.com (Amin Shali)
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "ElementsCountedInLoop",
    summary = "This code, which counts elements using a loop, can be replaced by a simpler library "
        + "method",
    explanation = "This code counts elements using a loop.  You can use various library methods "
        + "(Guava's Iterables.size(), Collection.size(), array.length) to achieve the same thing "
        + "in a cleaner way.",
    category = JDK, severity = WARNING, maturity = MATURE)
public class ElementsCountedInLoop extends BugChecker
    implements EnhancedForLoopTreeMatcher, WhileLoopTreeMatcher {

  @Override
  public Description matchWhileLoop(WhileLoopTree tree, VisitorState state) {
    JCWhileLoop whileLoop = (JCWhileLoop) tree;
    JCExpression whileExpression = ((JCParens) whileLoop.getCondition()).getExpression();
    if (whileExpression instanceof MethodInvocationTree) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) whileExpression;
      if (methodSelect(isDescendantOfMethod("java.util.Iterator", "hasNext()")).matches(
          methodInvocation, state)) {
        IdentifierTree identifier = getIncrementedIdentifer(extractSingleStatement(whileLoop.body));
        if (identifier != null) {
          return describeMatch(tree);
        }
      }
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchEnhancedForLoop(EnhancedForLoopTree tree, VisitorState state) {
    JCEnhancedForLoop enhancedForLoop = (JCEnhancedForLoop) tree;
    IdentifierTree identifier =
        getIncrementedIdentifer(extractSingleStatement(enhancedForLoop.body));
    if (identifier != null) {
      ExpressionTree expression = tree.getExpression();
      Fix fix;
      if (isSubtypeOf("java.util.Collection").matches(expression, state)) {
        String replacement = identifier + " += " + expression + ".size();";
        fix = SuggestedFix.replace(tree, replacement);
      } else if (isArrayType().matches(expression, state)) {
        String replacement = identifier + " += " + expression + ".length;";
        fix = SuggestedFix.replace(tree, replacement);
      } else {
        String replacement = identifier + " += Iterables.size(" + expression + ");";
        fix = SuggestedFix.builder()
            .replace(tree, replacement)
            .addImport("com.google.common.collect.Iterables")
            .build();
      }
      return describeMatch(tree, fix);
    }
    return Description.NO_MATCH;
  }

  /**
   * @return the only statement in a block containing one statement or the body itself when it is
   *         not a block.
   */
  private JCStatement extractSingleStatement(JCStatement body) {
    if (body.getKind() != Kind.BLOCK) {
      return body;
    }
    JCBlock block = (JCBlock) body;
    if (block.getStatements().size() == 1) {
      return block.getStatements().get(0);
    }
    return null;
  }

  /**
   * @return identifier which is being incremented by constant one. Returns null if no such
   *         identifier is found.
   */
  private IdentifierTree getIncrementedIdentifer(JCStatement statement) {
    if (statement == null) {
      return null;
    }
    if (statement.getKind() == Kind.EXPRESSION_STATEMENT) {
      Tree.Kind kind = ((JCExpressionStatement) statement).getExpression().getKind();
      if (kind == Kind.PREFIX_INCREMENT || kind == Kind.POSTFIX_INCREMENT) {
        JCUnary unary = (JCUnary) ((JCExpressionStatement) statement).getExpression();
        if (unary.arg.getKind() == Kind.IDENTIFIER) {
          return (IdentifierTree) unary.arg;
        }
        return null;
      } else if (kind == Kind.PLUS_ASSIGNMENT) {
        JCAssignOp assignOp = (JCAssignOp) ((JCExpressionStatement) statement).getExpression();
        if (assignOp.lhs.getKind() == Kind.IDENTIFIER && (isConstantOne(assignOp.rhs))) {
          return (IdentifierTree) assignOp.lhs;
        }
      } else if (kind == Kind.ASSIGNMENT) {
        JCAssign assign = (JCAssign) ((JCExpressionStatement) statement).getExpression();
        if (assign.lhs.getKind() == Kind.IDENTIFIER && assign.rhs.getKind() == Kind.PLUS) {
          JCBinary binary = (JCBinary) assign.rhs;
          if (binary.lhs.getKind() == Kind.IDENTIFIER) {
            if (((JCIdent) assign.lhs).sym == ((JCIdent) binary.lhs).sym) {
              if (isConstantOne(binary.rhs)) {
                return (IdentifierTree) binary.lhs;
              }
            }
          }
          if (binary.rhs.getKind() == Kind.IDENTIFIER) {
            if (((JCIdent) assign.lhs).sym == ((JCIdent) binary.rhs).sym) {
              if (isConstantOne(binary.lhs)) {
                return (IdentifierTree) binary.rhs;
              }
            }
          }
        }
      }
    }
    return null;
  }

  private boolean isConstantOne(JCExpression exp) {
    Tree.Kind kind = exp.getKind();

    if (kind == Kind.INT_LITERAL || kind == Kind.LONG_LITERAL || kind == Kind.FLOAT_LITERAL
        || kind == Kind.DOUBLE_LITERAL) {
      if (exp instanceof LiteralTree) {
        Object literalValue = ((LiteralTree) exp).getValue();
        if (literalValue instanceof Number) {
          int intValue = ((Number) literalValue).intValue();
          if (intValue == 1) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
