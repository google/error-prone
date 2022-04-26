/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.targetType;
import static java.lang.String.format;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;

/** A BugPattern; see the summary. */
@BugPattern(
    summary = "This calculation may lose precision compared to its target type.",
    severity = WARNING)
public final class NarrowCalculation extends BugChecker implements BinaryTreeMatcher {

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    var leftType = getType(tree.getLeftOperand());
    var rightType = getType(tree.getRightOperand());
    var targetType = targetType(state);
    if (leftType == null || rightType == null || targetType == null) {
      return NO_MATCH;
    }
    if (tree.getKind().equals(Kind.DIVIDE)
        && leftType.isIntegral()
        && rightType.isIntegral()
        && isFloatingPoint(targetType.type())) {
      Object leftConst = constValue(tree.getLeftOperand());
      Object rightConst = constValue(tree.getRightOperand());
      if (leftConst != null && rightConst != null) {
        long left = ((Number) leftConst).longValue();
        long right = ((Number) rightConst).longValue();
        long divided = left / right;
        if (divided * right == left) {
          return NO_MATCH;
        }
      }
      return buildDescription(tree)
          .setMessage(
              "This division will discard the fractional part of the result, despite being assigned"
                  + " to a float.")
          .addFix(
              SuggestedFix.builder()
                  .setShortDescription("Perform the division using floating point arithmetic")
                  .merge(forceExpressionType(tree, targetType.type(), state))
                  .build())
          .build();
    }
    if (tree.getKind().equals(Kind.MULTIPLY)
        && isInt(leftType, state)
        && isInt(rightType, state)
        && isLong(targetType.type(), state)) {
      // Heuristic: test data often gets generated with stuff like `i * 1000`, and is known not to
      // overflow.
      if (state.errorProneOptions().isTestOnlyTarget()) {
        return NO_MATCH;
      }
      var leftConst = constValue(tree.getLeftOperand());
      var rightConst = constValue(tree.getRightOperand());
      if (leftConst != null && rightConst != null) {
        int leftInt = (int) leftConst;
        int rightInt = (int) rightConst;
        long product = ((long) leftInt) * ((long) rightInt);
        if (product == (int) product) {
          return NO_MATCH;
        }
      }
      return buildDescription(tree)
          .setMessage(
              "This product of integers could overflow before being implicitly cast to a long.")
          .addFix(
              SuggestedFix.builder()
                  .setShortDescription("Perform the multiplication as long * long")
                  .merge(forceExpressionType(tree, targetType.type(), state))
                  .build())
          .build();
    }
    return NO_MATCH;
  }

  private static SuggestedFix forceExpressionType(
      BinaryTree tree, Type targetType, VisitorState state) {
    if (tree.getRightOperand() instanceof LiteralTree) {
      return SuggestedFix.replace(
          tree.getRightOperand(), forceLiteralType(tree.getRightOperand(), targetType, state));
    }
    if (tree.getLeftOperand() instanceof LiteralTree) {
      return SuggestedFix.replace(
          tree.getLeftOperand(), forceLiteralType(tree.getLeftOperand(), targetType, state));
    }
    return replace(
        tree.getRightOperand(),
        format("((%s) %s)", targetType, state.getSourceForNode(tree.getRightOperand())));
  }

  private static String forceLiteralType(ExpressionTree tree, Type targetType, VisitorState state) {
    return state.getSourceForNode(tree).replaceAll("[LlFfDd]$", "")
        + suffixForType(targetType, state);
  }

  private static String suffixForType(Type type, VisitorState state) {
    Symtab symtab = state.getSymtab();
    if (isSameType(type, symtab.longType, state)) {
      return "L";
    }
    if (isSameType(type, symtab.floatType, state)) {
      return "f";
    }
    if (isSameType(type, symtab.doubleType, state)) {
      return ".0";
    }
    throw new AssertionError();
  }

  private static boolean isFloatingPoint(Type type) {
    return type.isNumeric() && !type.isIntegral();
  }

  private static boolean isInt(Type type, VisitorState state) {
    return isSameType(type, state.getSymtab().intType, state);
  }

  private static boolean isLong(Type type, VisitorState state) {
    return isSameType(type, state.getSymtab().longType, state);
  }
}
