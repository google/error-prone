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

import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree.JCLiteral;

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
public class UnneededConditionalOperator extends DescribingMatcher<ConditionalExpressionTree> {

  private static final Matcher<ConditionalExpressionTree> matcher =
      new Matcher<ConditionalExpressionTree>() {
        @Override
        public boolean matches(ConditionalExpressionTree t, VisitorState state) {
          return t.getTrueExpression().getKind() == Kind.BOOLEAN_LITERAL &&
              t.getFalseExpression().getKind() == Kind.BOOLEAN_LITERAL;
        }
      };

  @Override
  public boolean matches(ConditionalExpressionTree t, VisitorState state) {
    return matcher.matches(t, state);
  }

  @Override
  public Description describe(ConditionalExpressionTree t, VisitorState state) {
    JCLiteral trueExpr = (JCLiteral) t.getTrueExpression();
    JCLiteral falseExpr = (JCLiteral) t.getFalseExpression();
    boolean trueExprValue = (Boolean) trueExpr.getValue();
    boolean falseExprValue = (Boolean) falseExpr.getValue();
    SuggestedFix fix;
    if (trueExprValue && !falseExprValue) {
      fix = new SuggestedFix().replace(t, t.getCondition().toString());
    } else if (!trueExprValue && falseExprValue) {
      // TODO(sjnickerson): More elegant negation here would be nice.
      // e.g. (isFoo()) -> (!isFoo()) rather than !((isFoo())
      //      a > b     ->  a <= b    rather than !(a > b)
      //      !a && b   ->  a || !b   rather than !(!a && b)
      fix = new SuggestedFix().replace(t, "!(" + t.getCondition().toString() + ")" + ")");
    } else {
      // trueExprValue == falseExprValue
      // Our suggested fix ignores any possible side-effects of the condition.
      fix = new SuggestedFix().replace(t, Boolean.toString(trueExprValue));
    }
    return new Description(t, diagnosticMessage, fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private final DescribingMatcher<ConditionalExpressionTree> describingMatcher
        = new UnneededConditionalOperator();

    @Override
    public Void visitConditionalExpression(
        ConditionalExpressionTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, describingMatcher);
      return super.visitConditionalExpression(node, visitorState);
    }
  }
}
