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
import static com.google.errorprone.bugpatterns.BugChecker.ConditionalExpressionTreeMatcher;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCUnary;

import java.util.HashMap;
import java.util.Map;

/**
 * Fix unnecessary uses of the conditional operator ?: when the true and false expressions are
 * just boolean literals.
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
@BugPattern(name = "UnneededConditionalOperator",
    summary = "You don't need a conditional operator if the conditional values are true and false",
    explanation = "An expression of the form isFoo() ? true : false is needlessly wordy. You can "
        + "skip the conditional operator entirely",
    category = JDK, severity = WARNING, maturity = EXPERIMENTAL)
public class UnneededConditionalOperator extends BugChecker implements ConditionalExpressionTreeMatcher {

  private static final Matcher<ConditionalExpressionTree> matcher =
      new Matcher<ConditionalExpressionTree>() {
        @Override
        public boolean matches(ConditionalExpressionTree t, VisitorState state) {
          return t.getTrueExpression().getKind() == Kind.BOOLEAN_LITERAL &&
              t.getFalseExpression().getKind() == Kind.BOOLEAN_LITERAL;
        }
      };

  private static Map<String, String> OPERATOR_OPPOSITES = new HashMap<String, String>();

  static {
    OPERATOR_OPPOSITES.put("<", ">=");
    OPERATOR_OPPOSITES.put("<=", ">");
    OPERATOR_OPPOSITES.put(">", "<=");
    OPERATOR_OPPOSITES.put(">=", "<");
    OPERATOR_OPPOSITES.put("==", "!=");
    OPERATOR_OPPOSITES.put("!=", "==");
  }

  @Override
  public Description matchConditionalExpression(ConditionalExpressionTree t, VisitorState state) {
    if (!matcher.matches(t, state)) {
      return Description.NO_MATCH;
    }

    JCLiteral trueExpr = (JCLiteral) t.getTrueExpression();
    JCLiteral falseExpr = (JCLiteral) t.getFalseExpression();
    boolean trueExprValue = (Boolean) trueExpr.getValue();
    boolean falseExprValue = (Boolean) falseExpr.getValue();
    SuggestedFix fix;
    if (trueExprValue && !falseExprValue) {
      fix = new SuggestedFix().replace(t, t.getCondition().toString());
    } else if (!trueExprValue && falseExprValue) {
      fix = new SuggestedFix().replace(t, negate(t.getCondition()));
    } else {
      // trueExprValue == falseExprValue
      // Our suggested fix ignores any possible side-effects of the condition.
      fix = new SuggestedFix().replace(t, Boolean.toString(trueExprValue));
    }
    return describeMatch(t, fix);
  }

  /**
   * Provides a string representation of the negation of a conditional expression, trying to
   * simplify as much as possible.
   *
   * We have special handling for parenthesised expressions (final expression will be parenthesised
   * too), binary expressions (signs are swapped for simple comparisons, de Morgan's laws are used
   * for && and ||) and unary expressions (!x becomes x rather than !(!x)).
   */
  private String negate(ExpressionTree cond) {
    if (cond instanceof ParenthesizedTree) {
      return "(" + negate(((ParenthesizedTree) cond).getExpression()) + ")";
    } else if (cond instanceof JCBinary) {
      JCBinary binary = (JCBinary) cond;
      String operator = binary.getOperator().getSimpleName().toString();
      String opposite = OPERATOR_OPPOSITES.get(operator);
      if (opposite != null) {
        return String.format("%s %s %s", binary.lhs.toString(), opposite, binary.rhs.toString());
      } else if (operator.equals("&&")) {
        return String.format("%s || %s", negate(binary.lhs), negate(binary.rhs));
      } else if (operator.equals("||")) {
        return String.format("%s && %s", negate(binary.lhs), negate(binary.rhs));
      } else {
        return "!(" + cond.toString() + ")";
      }
    } else if (cond instanceof JCUnary) {
      JCUnary unary = (JCUnary) cond;
      String operator = unary.getOperator().getSimpleName().toString();
      if (operator.equals("!")) {
        return unary.getExpression().toString();
      } else {
        return "!(" + cond.toString() + ")";
      }
    } else {
      return "!" + cond.toString();
    }
  }
}
