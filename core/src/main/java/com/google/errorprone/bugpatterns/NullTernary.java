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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ConditionalExpressionTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.Tree.Kind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "NullTernary",
    summary =
        "This conditional expression may evaluate to null, which will result in an NPE when the"
            + " result is unboxed.",
    severity = ERROR,
    category = JDK)
public class NullTernary extends BugChecker implements ConditionalExpressionTreeMatcher {

  @Override
  public Description matchConditionalExpression(
      ConditionalExpressionTree conditionalExpression, VisitorState state) {
    if (conditionalExpression.getFalseExpression().getKind() != Kind.NULL_LITERAL
        && conditionalExpression.getTrueExpression().getKind() != Kind.NULL_LITERAL) {
      return NO_MATCH;
    }
    ASTHelpers.TargetType targetType = ASTHelpers.targetType(state);
    if (targetType == null || !targetType.type().isPrimitive()) {
      return NO_MATCH;
    }
    return describeMatch(conditionalExpression);
  }
}
