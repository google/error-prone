/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.FRAGILE_CODE;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import javax.lang.model.type.TypeKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "IntLongMath",
  summary = "Expression of type int may overflow before being assigned to a long",
  severity = WARNING,
  category = JDK,
  tags = FRAGILE_CODE,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class IntLongMath extends BugChecker
    implements VariableTreeMatcher, AssignmentTreeMatcher, ReturnTreeMatcher {

  @Override
  public Description matchReturn(ReturnTree tree, VisitorState state) {
    if (tree.getExpression() == null) {
      return NO_MATCH;
    }
    Type type = null;
    outer:
    for (Tree parent : state.getPath()) {
      switch (parent.getKind()) {
        case METHOD:
          type = ASTHelpers.getType(((MethodTree) parent).getReturnType());
          break outer;
        case LAMBDA_EXPRESSION:
          type = state.getTypes().findDescriptorType(ASTHelpers.getType(parent)).getReturnType();
          break outer;
        default: // fall out
      }
    }
    if (type == null) {
      return NO_MATCH;
    }
    return check(type, tree.getExpression());
  }

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    return check(ASTHelpers.getType(tree), tree.getExpression());
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    return check(ASTHelpers.getType(tree), tree.getInitializer());
  }

  Description check(Type targetType, ExpressionTree init) {
    if (init == null) {
      return NO_MATCH;
    }
    if (ASTHelpers.constValue(init) != null) {
      return NO_MATCH;
    }
    if (targetType.getKind() != TypeKind.LONG) {
      return NO_MATCH;
    }
    // Note that we don't care about boxing as int isn't assignable to Long;
    // primtive widening conversions can't be combined with autoboxing.
    if (ASTHelpers.getType(init).getKind() != TypeKind.INT) {
      return NO_MATCH;
    }
    BinaryTree innerMost = null;
    ExpressionTree nested = init;
    while (true) {
      nested = ASTHelpers.stripParentheses(nested);
      if (!(nested instanceof BinaryTree)) {
        break;
      }
      switch (nested.getKind()) {
        case DIVIDE:
        case REMAINDER:
        case AND:
        case XOR:
        case OR:
        case RIGHT_SHIFT:
          // Skip binops that can't overflow; it doesn't matter whether they're performed on
          // longs or ints.
          break;
        default:
          innerMost = (BinaryTree) nested;
      }
      nested = ((BinaryTree) nested).getLeftOperand();
    }
    if (innerMost == null) {
      return NO_MATCH;
    }
    if (innerMost.getLeftOperand().getKind() == Kind.INT_LITERAL) {
      return describeMatch(init, SuggestedFix.postfixWith(innerMost.getLeftOperand(), "L"));
    }
    if (innerMost.getRightOperand().getKind() == Kind.INT_LITERAL) {
      return describeMatch(init, SuggestedFix.postfixWith(innerMost.getRightOperand(), "L"));
    }
    return describeMatch(init, SuggestedFix.prefixWith(innerMost.getLeftOperand(), "(long) "));
  }
}
