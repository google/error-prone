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
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;

/**
 * @author bill.pugh@gmail.com (Bill Pugh)
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "BadShiftAmount",
    summary = "Shift by an amount that is out of range",
    // TODO(eaftan): Better explanation
    explanation = "A 32-bit integer is shifted by the constant value 32. Since the shift "
        + "operator only uses the low 5 bits of the shift amount, shifting by 32 is a no-op.\n\n"
        + "See Java Language Specification 15.19 for more details.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
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
      Object rightValue = ((LiteralTree) rightOperand).getValue();
      if (!(rightOperand instanceof LiteralTree) || !(rightValue instanceof Number)) {
        return false;
      }
      int intValue = ((Number) rightValue).intValue();
      return intValue < 0 || intValue > 31;
    }
  };

  /**
   * Matches if the left operand is a long and the right operand is a literal that is not in the
   * range 0-63 inclusive.
   *
   * TODO(eaftan): Consider removing long case since none of those matched in Google code, and
   * there is no clear suggested fix.
   */
  private static final Matcher<BinaryTree> BAD_SHIFT_AMOUNT_LONG = new Matcher<BinaryTree>() {
    @Override
    public boolean matches(BinaryTree tree, VisitorState state) {
      Type leftType = ((JCTree) tree.getLeftOperand()).type;
      if (!(state.getTypes().isSameType(leftType, state.getSymtab().longType))) {
        return false;
      }

      ExpressionTree rightOperand = tree.getRightOperand();
      Object rightValue = ((LiteralTree) rightOperand).getValue();
      if (!(rightOperand instanceof LiteralTree) || !(rightValue instanceof Number)) {
        return false;
      }
      int intValue = ((Number) rightValue).intValue();
      return intValue < 0 || intValue > 63;
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
        anyOf(
            BAD_SHIFT_AMOUNT_INT,
            BAD_SHIFT_AMOUNT_LONG)
        ).matches(tree, state);
  }

  @Override
  public Description describe(BinaryTree tree, VisitorState state) {
    String replacement = String.format("(long) %s %s %s",  tree.getLeftOperand(), getOperator(tree.getKind()), tree.getRightOperand());
    SuggestedFix fix = new SuggestedFix().replace(tree, replacement);
    return new Description(tree, diagnosticMessage, fix);
  }

  public String getOperator(Kind kind) {
    switch (kind) {
      case RIGHT_SHIFT:
        return ">>";
      case UNSIGNED_RIGHT_SHIFT:
        return ">>>";
      case LEFT_SHIFT:
        return "<<";
      default:
        throw new IllegalArgumentException("Bad kind: " + kind);
    }
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
