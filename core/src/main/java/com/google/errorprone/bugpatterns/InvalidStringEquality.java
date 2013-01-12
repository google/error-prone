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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.kindIs;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.EQUAL_TO;
import static com.sun.source.tree.Tree.Kind.NOT_EQUAL_TO;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

import java.lang.StringBuilder;
/**
 * @author ptoomey@google.com (Patrick Toomey)
 */
@BugPattern(name = "StringEquality",
    summary = "String comparison using reference equality instead of value equality",
    explanation = "Strings are compared for reference equality/inequality using == or !="
        + "instead of for value equality using .equals()",
    category = JDK, severity = WARNING, maturity = EXPERIMENTAL)
public class InvalidStringEquality extends DescribingMatcher<BinaryTree> {

  private static final Matcher<BinaryTree> STRING_OPERANDS = new Matcher<BinaryTree>() {
    @Override
    public boolean matches(BinaryTree tree, VisitorState state) {
      Type stringType = state.getSymtab().stringType; 
      ExpressionTree leftOperand = tree.getLeftOperand();
      Type leftType = ((JCTree.JCExpression) leftOperand).type;
      // The left operand is not a String (ex. null) so no match 
      if (!state.getTypes().isSameType(leftType,stringType)) {
        return false;
      }
      ExpressionTree rightOperand = tree.getRightOperand();
      Type rightType = ((JCTree.JCExpression) rightOperand).type;
      // We know that both operands are String objects 
      if (state.getTypes().isSameType(rightType, stringType)) {
        return true;
      }
      return false;
    }
  };

  /* Match string that are compared with == and != */
  @Override  
  public boolean matches(BinaryTree tree, VisitorState state) {
    return allOf(
        anyOf(
            kindIs(EQUAL_TO),
            kindIs(NOT_EQUAL_TO)),
        STRING_OPERANDS
    ).matches(tree, state);
  }
  
  @Override
  public Description describe(BinaryTree tree, VisitorState state) {
    // TODO: Suggest fixes for more situations.
    ExpressionTree leftOperand = tree.getLeftOperand();
    Type leftType = ((JCTree.JCExpression) leftOperand).type;
    ExpressionTree rightOperand = tree.getRightOperand();
    Type rightType = ((JCTree.JCExpression) rightOperand).type;
    StringBuilder fixedExpression = new StringBuilder();
    // We maintain the ordering of the operands unless the leftOperand is not a constant
    // expression and the rightOperand is.
    if (leftType.constValue() == null && rightType.constValue() != null) {
      ExpressionTree tempOperand = leftOperand;
      leftOperand = rightOperand;
      rightOperand = tempOperand;
    }
    if (tree.getKind() == Tree.Kind.NOT_EQUAL_TO) {
      fixedExpression.append("!"); 
    }
    if (leftOperand instanceof BinaryTree) {
      fixedExpression.append("(" + leftOperand.toString() + ")"); 
    } else {
      fixedExpression.append(leftOperand.toString());
    }
    fixedExpression.append(".equals(" + rightOperand.toString() + ")");
    
    SuggestedFix fix = new SuggestedFix().replace(tree, fixedExpression.toString());
    return new Description(tree, diagnosticMessage, fix); 
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private InvalidStringEquality matcher = new InvalidStringEquality();

    @Override
    public Void visitBinary(BinaryTree tree, VisitorState visitorState) {
      evaluateMatch(tree, visitorState, matcher);
      return super.visitBinary(tree, visitorState);
    }
  }
}
