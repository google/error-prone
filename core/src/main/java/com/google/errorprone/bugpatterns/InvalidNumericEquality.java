/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.binaryTree;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.not;

import static com.sun.source.tree.Tree.Kind.EQUAL_TO;
import static com.sun.source.tree.Tree.Kind.NOT_EQUAL_TO;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;

/**
 * @author scottjohnson@google.com (Scott Johnson)
 */
@BugPattern(name = "NumericEquality",
    summary = "Numeric comparison using reference equality instead of value equality",
    explanation = "Numbers are compared for reference equality/inequality using == or != "
        + "instead of for value equality using .equals()",
    category = JDK, severity = ERROR, maturity = MATURE)
public class InvalidNumericEquality extends DescribingMatcher<BinaryTree> {

  @SuppressWarnings("unchecked")
  public static final Matcher<ExpressionTree> SUBCLASS_OF_NUMBER =
      allOf(isSubtypeOf("java.lang.Number"), not(kindIs(Tree.Kind.NULL_LITERAL)));
  
  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(BinaryTree tree, VisitorState state) {
    ExpressionTree leftOperand = tree.getLeftOperand();
    ExpressionTree rightOperand = tree.getRightOperand();
    Symbol left = ASTHelpers.getSymbol(leftOperand);
    Symbol right = ASTHelpers.getSymbol(rightOperand);
    if (left == null || right == null) {
      return false;
    }
    // Using a static final object as a sentinel is OK
    if ((isFinal(left) && left.isStatic()) || (isFinal(right) && right.isStatic())) {
      return false;
    }
    // Match left and right operand to subclasses of java.lang.Number and not null
    return allOf(anyOf(kindIs(EQUAL_TO), kindIs(NOT_EQUAL_TO)),
        binaryTree(SUBCLASS_OF_NUMBER, SUBCLASS_OF_NUMBER)).matches(tree, state);

  }

  public static boolean isFinal(Symbol s) {
    return (s.flags() & Flags.FINAL) != 0;
  }

  @Override
  public Description describe(BinaryTree tree, VisitorState state) {
    ExpressionTree leftOperand = tree.getLeftOperand();
    ExpressionTree rightOperand = tree.getRightOperand();
    StringBuilder fixedExpression = new StringBuilder();

    if (tree.getKind() == Tree.Kind.NOT_EQUAL_TO) {
      fixedExpression.append("!");
    }
    fixedExpression.append(
        "Objects.equal(" + leftOperand.toString() + ", " + rightOperand.toString() + ")");

    SuggestedFix fix = new SuggestedFix().replace(tree, fixedExpression.toString())
        .addImport("com.google.common.base.Objects");
    return new Description(tree, getDiagnosticMessage(), fix);
  }

  /**
   * Scanner for InvalidNumericEquality bugfix
   * @author scottjohnson@google.com (Scott Johnson)
   */
  public static class Scanner extends com.google.errorprone.Scanner {
    private InvalidNumericEquality matcher = new InvalidNumericEquality();

    @Override
    public Void visitBinary(BinaryTree tree, VisitorState visitorState) {
      evaluateMatch(tree, visitorState, matcher);
      return super.visitBinary(tree, visitorState);
    }
  }

}
