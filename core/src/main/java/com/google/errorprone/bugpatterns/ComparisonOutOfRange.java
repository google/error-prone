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
import static com.google.errorprone.matchers.Matchers.anyOf;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCLiteral;

/**
 * @author bill.pugh@gmail.com (Bill Pugh)
 *
 * TODO(eaftan): Support other types of comparisons?  Are there likely to be errors in those?
 * short might be a reasonable one to do since it will be widened to integer.
 */
@BugPattern(name = "ComparisonOutOfRange",
    // TODO(eaftan): Would be nice if error message gave the types being compared.
    summary = "Comparison of a value to another value that is out of range",
    explanation = "This checker looks for equality comparisons to values that are out of " +
        "range for the compared type.  For example, bytes can only have a value in the range " +
        Byte.MIN_VALUE + " to " + Byte.MAX_VALUE + ". Comparing a byte with a value outside " +
        "that range will always return false and usually indicates an error in the code.\n\n" +
        "This checker currently supports checking for bad byte and character comparisons.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class ComparisonOutOfRange extends DescribingMatcher<BinaryTree> {

  /**
   * Matches comparisons that are out of range for the given type.  Parameterized based on the
   * type of comparison (byte or char).
   *
   * TODO(eaftan): Can any of this be extracted to matchers library?
   */
  private static class BadComparisonMatcher implements Matcher<BinaryTree> {
    /**
     * The type of bad comparison matcher to create. Must be either Byte.TYPE or Character.TYPE.
     */
    private Class<?> type;

    private boolean initialized = false;
    private Type comparisonType;
    private int maxValue;
    private int minValue;

    public BadComparisonMatcher(Class<?> type) {
      if (type != Byte.TYPE && type != Character.TYPE) {
        throw new IllegalArgumentException("type must be either byte or char, but was "
            + type.getName());
      }
      this.type = type;
    }

    private void init(Symtab symbolTable) {
      if (initialized) {
        throw new IllegalStateException("Do not try to initialize twice!");
      }

      // Specialize matcher based on type.
      if (type == Byte.TYPE) {
        comparisonType = symbolTable.byteType;
        maxValue = Byte.MAX_VALUE;
        minValue = Byte.MIN_VALUE;
      } else {
        comparisonType = symbolTable.charType;
        maxValue = Character.MAX_VALUE;
        minValue = Character.MIN_VALUE;
      }
      initialized = true;
    }

    @Override
    public boolean matches(BinaryTree tree, VisitorState state) {
      if (!initialized) {
        init(state.getSymtab());
      }

      // Must be an == or != comparison.
      if (tree.getKind() != Kind.EQUAL_TO && tree.getKind() != Kind.NOT_EQUAL_TO) {
        return false;
      }

      // Match trees that have one operand of the specified type, and the other as a literal.
      ExpressionTree leftOperand = tree.getLeftOperand();
      Type leftType = ((JCTree) leftOperand).type;
      ExpressionTree rightOperand = tree.getRightOperand();
      Type rightType = ((JCTree) rightOperand).type;
      JCLiteral literal = null;
      if (state.getTypes().isSameType(leftType, comparisonType) &&
          rightOperand instanceof JCLiteral) {
        literal = (JCLiteral) rightOperand;
      } else if (leftOperand instanceof JCLiteral &&
            state.getTypes().isSameType(rightType, comparisonType)) {
          literal = (JCLiteral) leftOperand;
      } else {
        return false;
      }

      // Check whether literal is out of range for the specified type.  Logic is based on
      // JLS 5.6.2 - Binary Numeric Promotion:
      // If either is double, other is converted to double.
      // If either is float, other is converted to float.
      // If either is long, other is converted to long.
      // Otherwise, both are converted to int.
      switch (literal.getKind()) {
        case DOUBLE_LITERAL:
          double doubleValue = ((Double) literal.getValue()).doubleValue();
          return doubleValue < minValue || doubleValue > maxValue;
        case FLOAT_LITERAL:
          float floatValue = ((Float) literal.getValue()).floatValue();
          return floatValue < minValue || floatValue > maxValue;
        case LONG_LITERAL:
          long longValue = ((Long) literal.getValue()).longValue();
          return longValue < minValue || longValue > maxValue;
        default:
          int intValue = ((Integer) literal.getValue()).intValue();
          return intValue < minValue || intValue > maxValue;
      }
    }
  }

  private static final Matcher<BinaryTree> BYTE_MATCHER = new BadComparisonMatcher(Byte.TYPE);
  private static final Matcher<BinaryTree> CHAR_MATCHER = new BadComparisonMatcher(Character.TYPE);

  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(BinaryTree tree, VisitorState state) {
    return anyOf(BYTE_MATCHER, CHAR_MATCHER).matches(tree, state);
  }

  /**
   * Suggested fixes are as follows.  For the byte case, convert the literal to its byte
   * representation. For example, "255" becomes "-1.  For the character case, replace the
   * comparison with "false" since it's not clear what was intended.
   *
   * TODO(eaftan): Evaluate the suggested fix for the character case.  Can we do better than
   * "false"?
   */
  @Override
  public Description describe(BinaryTree tree, VisitorState state) {

    JCLiteral literal = null;
    boolean byteMatch;
    if (tree.getRightOperand() instanceof JCLiteral) {
      literal = (JCLiteral) tree.getRightOperand();
      byteMatch = state.getTypes().isSameType(((JCTree) tree.getLeftOperand()).type,
          state.getSymtab().byteType);
    } else if (tree.getLeftOperand() instanceof JCLiteral) {
      literal = (JCLiteral) tree.getLeftOperand();
      byteMatch = state.getTypes().isSameType(((JCTree) tree.getRightOperand()).type,
          state.getSymtab().byteType);
    } else {
      throw new IllegalStateException("Expected one of the operands to be a literal");
    }

    SuggestedFix fix = new SuggestedFix();
    if (byteMatch) {
      fix.replace(literal, Byte.toString(((Number) literal.getValue()).byteValue()));
    } else {
      fix.replace(tree, "false");
    }

    return new Description(tree, diagnosticMessage, fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private ComparisonOutOfRange matcher = new ComparisonOutOfRange();

    @Override
    public Void visitBinary(BinaryTree tree, VisitorState visitorState) {
      evaluateMatch(tree, visitorState, matcher);
      return super.visitBinary(tree, visitorState);
    }
  }

}
