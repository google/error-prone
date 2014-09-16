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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
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
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
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
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class NumericEquality extends BugChecker implements BinaryTreeMatcher {

  public static final Matcher<ExpressionTree> SUBCLASS_OF_NUMBER =
      allOf(isSubtypeOf("java.lang.Number"), not(kindIs(Tree.Kind.NULL_LITERAL)));
  public static final Matcher<BinaryTree> MATCHER = allOf(
      anyOf(kindIs(EQUAL_TO), kindIs(NOT_EQUAL_TO)),
      binaryTree(SUBCLASS_OF_NUMBER, SUBCLASS_OF_NUMBER));

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    ExpressionTree leftOperand = tree.getLeftOperand();
    ExpressionTree rightOperand = tree.getRightOperand();
    Symbol left = ASTHelpers.getSymbol(leftOperand);
    Symbol right = ASTHelpers.getSymbol(rightOperand);
    if (left == null || right == null) {
      return Description.NO_MATCH;
    }
    // Using a static final object as a sentinel is OK
    if ((isFinal(left) && left.isStatic()) || (isFinal(right) && right.isStatic())) {
      return Description.NO_MATCH;
    }
    // Match left and right operand to subclasses of java.lang.Number and not null
    if (!MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    StringBuilder fixedExpression = new StringBuilder();

    if (tree.getKind() == Tree.Kind.NOT_EQUAL_TO) {
      fixedExpression.append("!");
    }
    fixedExpression.append(
        "Objects.equal(" + leftOperand + ", " + rightOperand + ")");

    Fix fix = SuggestedFix.builder()
        .replace(tree, fixedExpression.toString())
        .addImport("com.google.common.base.Objects")
        .build();
    return describeMatch(tree, fix);
  }

  public static boolean isFinal(Symbol s) {
    return (s.flags() & Flags.FINAL) != 0;
  }
}
