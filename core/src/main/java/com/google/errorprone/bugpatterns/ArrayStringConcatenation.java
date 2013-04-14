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
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.binaryOperator;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;


/**
 * @author adgar@google.com (Mike Edgar)
 */
@BugPattern(name = "ArrayStringConcatenation",
    summary = "toString used on an array",
    explanation = "When concatenating an array to a string, the toString method on an array will " +
        "yield its identity, such as [I@4488aabb. This is almost never needed. Use " +
        "Arrays.toString to obtain a human-readable array summary.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class ArrayStringConcatenation extends DescribingMatcher<BinaryTree> {

  private static final Matcher<ExpressionTree> isArrayMatcher =
      Matchers.<ExpressionTree>isArrayType();

  @SuppressWarnings("unchecked")
  private static final Matcher<BinaryTree> concatenationMatcher = anyOf(
      binaryOperator(
          Kind.PLUS,
          isArrayMatcher,
          Matchers.<ExpressionTree>isSameType("java.lang.String")),
      binaryOperator(
          Kind.PLUS,
          Matchers.<ExpressionTree>isSameType("java.lang.String"),
          isArrayMatcher));

  /**
   * Matches strings added with arrays.
   */
  @Override
  public boolean matches(BinaryTree t, VisitorState state) {
    return concatenationMatcher.matches(t, state);
  }

  /**
   * Replaces instances of implicit array toString() calls due to string concatenation with
   * Arrays.toString(array). Also adds the necessary import statement for java.util.Arrays.
   */
  @Override
  public Description describe(BinaryTree t, VisitorState state) {
    final String replacement;
    String leftOperand = t.getLeftOperand().toString();
    String rightOperand = t.getRightOperand().toString();
    if (isArrayMatcher.matches(t.getLeftOperand(), state)) {
      replacement = "Arrays.toString(" + leftOperand + ") + " + rightOperand;
    } else {
      replacement = leftOperand + " + Arrays.toString(" + rightOperand + ")";
    }
    SuggestedFix fix = new SuggestedFix()
        .replace(t, replacement)
        .addImport("java.util.Arrays");
    return new Description(t, diagnosticMessage, fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    public DescribingMatcher<BinaryTree> scannerMatcher = new ArrayStringConcatenation();

    @Override
    public Void visitBinary(BinaryTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, scannerMatcher);
      return super.visitBinary(node, visitorState);
    }
  }
}
