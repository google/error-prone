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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.kindIs;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
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
@BugPattern(
  name = "BadShiftAmount",
  summary = "Shift by an amount that is out of range",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class BadShiftAmount extends BugChecker implements BinaryTreeMatcher {

  /**
   * Matches if the left operand is an int, byte, short, or char, and the right operand is a literal
   * that is not in the range 0-31 inclusive.
   *
   * <p>In a shift expression, byte, short, and char undergo unary numeric promotion and are
   * promoted to int. See JLS 5.6.1.
   */
  private static final Matcher<BinaryTree> BAD_SHIFT_AMOUNT_INT =
      new Matcher<BinaryTree>() {
        @Override
        public boolean matches(BinaryTree tree, VisitorState state) {
          Type leftType = ((JCTree) tree.getLeftOperand()).type;
          Types types = state.getTypes();
          Symtab symtab = state.getSymtab();
          if (!(types.isSameType(leftType, symtab.intType))
              && !(types.isSameType(leftType, symtab.byteType))
              && !(types.isSameType(leftType, symtab.shortType))
              && !(types.isSameType(leftType, symtab.charType))) {
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

  public static final Matcher<BinaryTree> BINARY_TREE_MATCHER =
      allOf(
          anyOf(
              kindIs(Kind.LEFT_SHIFT), kindIs(Kind.RIGHT_SHIFT), kindIs(Kind.UNSIGNED_RIGHT_SHIFT)),
          BAD_SHIFT_AMOUNT_INT);

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    if (!BINARY_TREE_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    /*
     * For shift amounts in [32, 63], cast the left operand to long.  Otherwise change the shift
     * amount to whatever would actually be used.
     */
    int intValue = ((Number) ((LiteralTree) tree.getRightOperand()).getValue()).intValue();

    Fix fix;
    if (intValue >= 32 && intValue <= 63) {
      if (tree.getLeftOperand().getKind() == Kind.INT_LITERAL) {
        fix = SuggestedFix.postfixWith(tree.getLeftOperand(), "L");
      } else {
        fix = SuggestedFix.prefixWith(tree, "(long) ");
      }
    } else {
      // This is the equivalent shift distance according to JLS 15.19.
      String actualShiftDistance = Integer.toString(intValue & 0x1f);
      fix = SuggestedFix.replace(tree.getRightOperand(), actualShiftDistance);
    }
    return describeMatch(tree, fix);
  }
}
