/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ConditionalExpressionTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ASTHelpers.TargetType;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;

/** @author awturner@google.com (Andy Turner) */
@BugPattern(
    name = "ConditionalExpressionNumericPromotion",
    summary =
        "A conditional expression with numeric operands of differing types will perform binary "
            + "numeric promotion of the operands; when these operands are of reference types, "
            + "the expression's result may not be of the expected type.",
    severity = ERROR,
    category = JDK,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class ConditionalExpressionNumericPromotion extends BugChecker
    implements ConditionalExpressionTreeMatcher {

  @Override
  public Description matchConditionalExpression(
      ConditionalExpressionTree conditionalExpression, VisitorState state) {
    Type expressionType = checkNotNull(ASTHelpers.getType(conditionalExpression));
    if (!expressionType.isPrimitive()) {
      return NO_MATCH;
    }

    ExpressionTree trueExpression = conditionalExpression.getTrueExpression();
    ExpressionTree falseExpression = conditionalExpression.getFalseExpression();

    Type trueType = checkNotNull(ASTHelpers.getType(trueExpression));
    Type falseType = checkNotNull(ASTHelpers.getType(falseExpression));
    if (trueType.isPrimitive() || falseType.isPrimitive()) {
      return NO_MATCH;
    }

    if (ASTHelpers.isSameType(trueType, falseType, state)) {
      return NO_MATCH;
    }

    TargetType targetType = ASTHelpers.targetType(state);
    if (targetType == null) {
      return NO_MATCH;
    }
    if (targetType.type().isPrimitive()) {
      return NO_MATCH;
    }

    Type numberType = state.getTypeFromString("java.lang.Number");
    if (ASTHelpers.isSubtype(targetType.type(), numberType, state)
        && !ASTHelpers.isSameType(targetType.type(), numberType, state)) {
      return NO_MATCH;
    }

    SuggestedFix.Builder builder = SuggestedFix.builder();
    String numberName = SuggestedFixes.qualifyType(state, builder, numberType);
    String prefix = "((" + numberName + ") ";
    builder.prefixWith(trueExpression, prefix).postfixWith(trueExpression, ")");
    builder.prefixWith(falseExpression, prefix).postfixWith(falseExpression, ")");
    return describeMatch(conditionalExpression, builder.build());
  }
}
