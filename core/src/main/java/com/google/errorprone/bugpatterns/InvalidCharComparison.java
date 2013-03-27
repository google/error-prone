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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.sun.source.tree.Tree.Kind.EQUAL_TO;
import static com.sun.source.tree.Tree.Kind.NOT_EQUAL_TO;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

import java.lang.StringBuilder;

/**
 * @author bill.pugh@gmail.com (Bill Pugh)
 */
@BugPattern(name = "CharComparison",
    summary = "Bad comparison of char",
    explanation = "Signed bytes can only have a value in the range " + Character.MIN_VALUE  + " to "
    + Character.MAX_VALUE + ". Comparing "
 + " a char with a value outside that range is vacuous and likely to be incorrect.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class InvalidCharComparison extends DescribingMatcher<BinaryTree> {

    private static  boolean match(ExpressionTree leftOperand,   ExpressionTree rightOperand,  VisitorState state) {
        Type byteType = state.getSymtab().charType;
        Type leftType = ((JCTree.JCExpression) leftOperand).type;
        if (!state.getTypes().isSameType(leftType, byteType)) {
          return false;
        }
        if (!(rightOperand instanceof LiteralTree))
            return false;
        LiteralTree rightLiteral = (LiteralTree) rightOperand;
        Object value = rightLiteral.getValue();
        if (!(value instanceof Number))
            return false;
        int intValue = ((Number) value).intValue() ;
        return intValue < Character.MIN_VALUE || intValue > Character.MAX_VALUE;
      }
  /**
   *  A {@link Matcher} that matches whether the operands in a {@link BinaryTree} are
   *  strictly String operands.  For Example, if either operand is {@code null} the matcher
   *  will return {@code false}
   */
  private static final Matcher<BinaryTree> BAD_CHAR_COMPARISON = new Matcher<BinaryTree>() {


    @Override
    public boolean matches(BinaryTree tree, VisitorState state) {

      return match(tree.getLeftOperand(), tree.getRightOperand(), state)
              ||  match(tree.getRightOperand(), tree.getLeftOperand(), state);
    }
  };

  /* Match string that are compared with == and != */
  @Override
  public boolean matches(BinaryTree tree, VisitorState state) {
    return allOf(
        anyOf(
            kindIs(EQUAL_TO),
            kindIs(NOT_EQUAL_TO)),
        BAD_CHAR_COMPARISON
    ).matches(tree, state);
  }

  @Override
  public Description describe(BinaryTree tree, VisitorState state) {

    ExpressionTree leftOperand = tree.getLeftOperand();
    ExpressionTree rightOperand = tree.getRightOperand();
    String fixedExpression;

    if (rightOperand instanceof LiteralTree)
        fixedExpression = fix(leftOperand, tree.getKind(), (LiteralTree) rightOperand);
    else     if (leftOperand instanceof LiteralTree)
        fixedExpression = fix(rightOperand, tree.getKind(), (LiteralTree) leftOperand);
    else throw new IllegalStateException();

    SuggestedFix fix = new SuggestedFix().replace(tree, fixedExpression.toString());
    return new Description(tree, diagnosticMessage, fix);
  }

  private String fix(ExpressionTree leftOperand, Kind kind,
          LiteralTree rightLiteral) {
      return "false";
  }

public static class Scanner extends com.google.errorprone.Scanner {
    private InvalidCharComparison matcher = new InvalidCharComparison();

    @Override
    public Void visitBinary(BinaryTree tree, VisitorState visitorState) {
      evaluateMatch(tree, visitorState, matcher);
      return super.visitBinary(tree, visitorState);
    }
  }

}
