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
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.sun.source.tree.Tree.Kind.EQUAL_TO;
import static com.sun.source.tree.Tree.Kind.NOT_EQUAL_TO;

import com.google.common.base.Joiner;
import com.google.errorprone.BugPattern;
import com.google.errorprone.JDKCompatible;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCLiteral;

/**
 * @author ptoomey@google.com (Patrick Toomey)
 */
@BugPattern(name = "StringEquality",
    summary = "String comparison using reference equality instead of value equality",
    explanation = "Strings are compared for reference equality/inequality using == or !="
        + "instead of for value equality using .equals()",
    category = JDK, severity = WARNING, maturity = MATURE)
public class StringEquality extends BugChecker implements BinaryTreeMatcher {

  /**
   *  A {@link Matcher} that matches whether the operands in a {@link BinaryTree} are
   *  strictly String operands.  For Example, if either operand is {@code null} the matcher
   *  will return {@code false}
   */
  private static final Matcher<BinaryTree> STRING_OPERANDS = new Matcher<BinaryTree>() {
    @Override
    public boolean matches(BinaryTree tree, VisitorState state) {
      Type stringType = state.getSymtab().stringType;
      ExpressionTree leftOperand = tree.getLeftOperand();
      Type leftType = ((JCTree.JCExpression) leftOperand).type;
      // The left operand is not a String (ex. null) so no match
      if (!state.getTypes().isSameType(leftType, stringType)) {
        return false;
      }
      ExpressionTree rightOperand = tree.getRightOperand();
      Type rightType = ((JCTree.JCExpression) rightOperand).type;
      // We know that both operands are String objects
      return state.getTypes().isSameType(rightType, stringType);
    }
  };

  public static final Matcher<BinaryTree> MATCHER = allOf(
      anyOf(kindIs(EQUAL_TO), kindIs(NOT_EQUAL_TO)),
      STRING_OPERANDS);

  /* Match string that are compared with == and != */
  @Override
  public Description matchBinary(BinaryTree tree, final VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder fix = SuggestedFix.builder();

    // Consider one of the tree's operands. If it is "", and the other is non-null,
    // then call isEmpty on the other.
    StringBuilder fixExpr = considerOneOf(tree.getLeftOperand(), tree.getRightOperand(),
        new HandleChoice<ExpressionTree, StringBuilder>() {
          @Override
          public StringBuilder apply(ExpressionTree it, ExpressionTree other) {
            return "".equals(getConstValue(it)) && isNonNull(other, state)
                   ? methodCall(other, "isEmpty") : null;
          }
        });

    if (fixExpr == null) {
      // Consider one of the tree's operands. If it is non-null,
      // then call equals on it, passing the other operand as argument.
      fixExpr = considerOneOf(tree.getLeftOperand(), tree.getRightOperand(),
        new HandleChoice<ExpressionTree, StringBuilder>() {
          @Override
          public StringBuilder apply(ExpressionTree it, ExpressionTree other) {
            return isNonNull(it, state) ? methodCall(it, "equals", other) : null;
          }
        });

      if (fixExpr == null) {
        fixExpr = methodCall(
            null, "Objects.equals", tree.getLeftOperand(), tree.getRightOperand());
        fix.addImport("java.util.Objects");
      }
    }

    if (tree.getKind() == Tree.Kind.NOT_EQUAL_TO) {
      fixExpr.insert(0, "!");
    }

    fix.replace(tree, fixExpr.toString());
    return describeMatch(tree, fix.build());
  }

  private static Object getConstValue(Tree tree) {
    return (tree instanceof JCLiteral)
        ? ((JCLiteral) tree).value
        : ((JCTree) tree).type.constValue();
  }

  private interface HandleChoice<T, R> {
    R apply(T it, T other);
  }

  private static <T, R> R considerOneOf(final T a, final T b, final HandleChoice<T, R> f) {
    R r = f.apply(a, b);
    return r == null ? f.apply(b, a) : r;
  }

  private static boolean isNonNull(ExpressionTree expr, VisitorState state) {
    return JDKCompatible.isDefinitelyNonNull(new TreePath(state.getPath(), expr), state.context);
  }

  /**
   * Create a method call {@code methodName} with parameters {@code params}.
   * If {@code receiver} is null, the call is static or to {@code this},
   * otherwise the call is to {@code receiver}.
   */
  private static StringBuilder methodCall(ExpressionTree receiver, String methodName,
      ExpressionTree... params) {
    final StringBuilder fixedExpression = new StringBuilder();
    if (receiver != null) {
      boolean isBinop = receiver instanceof BinaryTree;
      if (isBinop) {
        fixedExpression.append("(");
      }
      fixedExpression.append(receiver);
      if (isBinop) {
        fixedExpression.append(")");
      }
      fixedExpression.append(".");
    }
    fixedExpression.append(methodName);
    fixedExpression.append("(");
    fixedExpression.append(Joiner.on(", ").join(params));
    fixedExpression.append(")");

    return fixedExpression;
  }
}
