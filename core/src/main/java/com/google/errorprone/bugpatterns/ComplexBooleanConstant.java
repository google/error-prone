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

import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import java.util.Objects;

/** @author Sumit Bhagwani (bhagwani@google.com) */
@BugPattern(
    name = "ComplexBooleanConstant",
    summary = "Non-trivial compile time constant boolean expressions shouldn't be used.",
    category = Category.JDK,
    severity = SeverityLevel.ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class ComplexBooleanConstant extends BugChecker implements BinaryTreeMatcher {

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    Boolean constValue = booleanConstValue(tree);
    if (constValue == null) {
      return NO_MATCH;
    }
    return buildDescription(tree)
        .addFix(SuggestedFix.replace(tree, constValue.toString()))
        .setMessage(
            String.format(
                "This expression always evalutes to `%s`, prefer a boolean literal for clarity.",
                constValue))
        .build();
  }

  Boolean booleanConstValue(Tree tree) {
    return tree.accept(
        new SimpleTreeVisitor<Boolean, Void>() {
          @Override
          protected Boolean defaultAction(Tree node, Void unused) {
            return ASTHelpers.constValue(node, Boolean.class);
          }

          @Override
          public Boolean visitIdentifier(IdentifierTree node, Void aVoid) {
            // Ignore constant variables.
            return null;
          }

          @Override
          public Boolean visitMemberSelect(MemberSelectTree node, Void aVoid) {
            // Ignore constant variables.
            return null;
          }

          @Override
          public Boolean visitBinary(BinaryTree node, Void unused) {
            Boolean result = defaultAction(node, null);
            if (result != null
                && node.getLeftOperand().getKind() != Tree.Kind.IDENTIFIER
                && node.getRightOperand().getKind() != Tree.Kind.IDENTIFIER) {
              return result;
            }
            Boolean lhs = node.getLeftOperand().accept(this, null);
            Boolean rhs = node.getRightOperand().accept(this, null);
            switch (node.getKind()) {
              case CONDITIONAL_AND:
              case AND:
                if (lhs != null && rhs != null) {
                  return lhs && rhs;
                }
                if (Objects.equals(lhs, Boolean.FALSE) || Objects.equals(rhs, Boolean.FALSE)) {
                  return false;
                }
                break;
              case CONDITIONAL_OR:
              case OR:
                if (lhs != null && rhs != null) {
                  return lhs || rhs;
                }
                if (Objects.equals(lhs, Boolean.TRUE) || Objects.equals(rhs, Boolean.TRUE)) {
                  return true;
                }
                break;
              default: // fall out
            }
            return null;
          }
        },
        null);
  }
}
