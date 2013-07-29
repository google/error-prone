/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCLiteral;

/**
 * @author bill.pugh@gmail.com (Bill Pugh)
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "BadShiftAmount",
    summary = "Shift by an amount that is out of range",
    explanation = "For shift operations on int types, only the five lowest-order bits of the "
        + "shift amount are used as the shift distance.  This means that shift amounts that are "
        + "not in the range 0 to 31, inclusive, are silently mapped to values in that range. "
        + "For example, a shift of an int by 32 is equivalent to shifting by 0, i.e., a no-op.\n\n"
        + "See JLS 15.19, \"Shift Operators\", for more details.",
    category = JDK, severity = ERROR, maturity = MATURE)
public class BadShiftAmount extends DescribingMatcher<BinaryTree> {

  /**
   * Matches if the left operand is an int, byte, short, or char, and the right operand is a
   * literal that is not in the range 0-31 inclusive.
   *
   * <p>In a shift expression, byte, short, and char undergo unary numeric promotion and are
   * promoted to int.  See JLS 5.6.1.
   */
  private static final Matcher<BinaryTree> BAD_SHIFT_AMOUNT_INT = new Matcher<BinaryTree>() {
    @Override
    public boolean matches(BinaryTree tree, VisitorState state) {
      Type leftType = ((JCTree) tree.getLeftOperand()).type;
      Types types = state.getTypes();
      Symtab symtab = state.getSymtab();
      if (!(types.isSameType(leftType, symtab.intType)) &&
          !(types.isSameType(leftType, symtab.byteType)) &&
          !(types.isSameType(leftType, symtab.shortType)) &&
          !(types.isSameType(leftType, symtab.charType))) {
        return false;
      }

      ExpressionTree rightOperand = tree.getRightOperand();
      if (rightOperand instanceof LiteralTree) {
        Object rightValue = ((LiteralTree) rightOperand).getValue();
        if (rightValue instanceof Number) {
          int intValue = ((Number) rightValue).intValue();
          return intValue < 0 || intValue > 31;
        }
      }

      return false;
    }
  };

  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(BinaryTree tree, VisitorState state) {
    return allOf(
        anyOf(
            kindIs(Kind.LEFT_SHIFT),
            kindIs(Kind.RIGHT_SHIFT),
            kindIs(Kind.UNSIGNED_RIGHT_SHIFT)),
        BAD_SHIFT_AMOUNT_INT
        ).matches(tree, state);
  }

  /**
   * For shift amounts in [32, 63], cast the left operand to long.  Otherwise change the shift
   * amount to whatever would actually be used.
   */
  @Override
  public Description describe(BinaryTree tree, VisitorState state) {
    int intValue = ((Number) ((LiteralTree) tree.getRightOperand()).getValue()).intValue();

    SuggestedFix fix = new SuggestedFix();
    if (intValue >= 32 && intValue <= 63) {
      if (tree.getLeftOperand().getKind() == Kind.INT_LITERAL) {
        fix = fix.postfixWith(tree.getLeftOperand(), "L");
      } else {
        fix = fix.prefixWith(tree, "(long) ");
      }
    } else {
      JCLiteral jcLiteral = (JCLiteral) tree.getRightOperand();
      // This is the equivalent shift distance according to JLS 15.19.
      String actualShiftDistance = Integer.toString(intValue & 0x1f);
      int actualStart = ASTHelpers.getActualStartPosition(jcLiteral, state.getSourceCode());
      if (actualStart != jcLiteral.getStartPosition()) {
        fix = fix.replace(tree.getRightOperand(), actualShiftDistance,
            actualStart - jcLiteral.getStartPosition(), 0);
      } else {
        fix = fix.replace(tree.getRightOperand(), actualShiftDistance);
      }
    }
    return new Description(tree, getDiagnosticMessage(), fix);
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
