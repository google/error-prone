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

/**
 * @author bill.pugh@gmail.com (Bill Pugh)
 */
@BugPattern(name = "BadShiftAmount",
    summary = "Bad shift of 32-bit int",
    explanation = "A 32-bit integer is shifted by the constant value 32. Since the shift operator only uses the low"
    + " 5 bits of the shift amount, shifting by 32 is a no-op",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class BadShiftAmount extends DescribingMatcher<BinaryTree> {

    /**
   *  A {@link Matcher} that matches whether the operands in a {@link BinaryTree} are
   *  strictly String operands.  For Example, if either operand is {@code null} the matcher
   *  will return {@code false}  
   */
  private static final Matcher<BinaryTree> BAD_SHIFT_AMOUNT_COMPARISON = new Matcher<BinaryTree>() {

      
    @Override
    public boolean matches(BinaryTree tree, VisitorState state) {
      
      ExpressionTree rightOperand = tree.getRightOperand();
    Type intType = state.getSymtab().intType; 
    Type leftType = ((JCTree.JCExpression) tree.getLeftOperand()).type;
    if (!state.getTypes().isSameType(leftType, intType)) {
      return false;
    }
    if (!(rightOperand instanceof LiteralTree))
        return false;
    LiteralTree rightLiteral = (LiteralTree) rightOperand;
    Object value = rightLiteral.getValue();
    if (!(value instanceof Number))
        return false;
    int intValue = ((Number) value).intValue() ;
    return intValue == 32;
             
    }
  };

  /* Match string that are compared with == and != */
  @Override  
  public boolean matches(BinaryTree tree, VisitorState state) {
    return allOf(
        anyOf(
            kindIs(Tree.Kind.LEFT_SHIFT),
            kindIs(Tree.Kind.RIGHT_SHIFT),
            kindIs(Tree.Kind.UNSIGNED_RIGHT_SHIFT)),
        BAD_SHIFT_AMOUNT_COMPARISON
    ).matches(tree, state);
  }
  
  @Override
  public Description describe(BinaryTree tree, VisitorState state) {

    ExpressionTree leftOperand = tree.getLeftOperand();
    
    
    SuggestedFix fix = new SuggestedFix().replace(tree, leftOperand.toString());
    return new Description(tree, diagnosticMessage, fix); 
  }

  
public static class Scanner extends com.google.errorprone.Scanner {
    private BadShiftAmount matcher = new BadShiftAmount();

    @Override
    public Void visitBinary(BinaryTree tree, VisitorState visitorState) {
      evaluateMatch(tree, visitorState, matcher);
      return super.visitBinary(tree, visitorState);
    }
  }

}
