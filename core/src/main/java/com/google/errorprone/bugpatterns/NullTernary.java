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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.base.Preconditions;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ConditionalExpressionTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "NullTernary",
  summary =
      "This conditional expression may evaluate to null, which will result in an NPE when the"
          + " result is unboxed.",
  severity = ERROR,
  category = JDK
)
public class NullTernary extends BugChecker implements ConditionalExpressionTreeMatcher {

  @Override
  public Description matchConditionalExpression(
      ConditionalExpressionTree conditionalExpression, VisitorState state) {
    if (conditionalExpression.getFalseExpression().getKind() != Kind.NULL_LITERAL
        && conditionalExpression.getTrueExpression().getKind() != Kind.NULL_LITERAL) {
      return NO_MATCH;
    }
    TreePath path = state.getPath();
    do {
      path = path.getParentPath();
    } while (path != null && path.getLeaf().getKind() == Kind.PARENTHESIZED);
    if (path == null) {
      return NO_MATCH;
    }
    Tree parent = path.getLeaf();
    Type type =
        parent.accept(
            new SimpleTreeVisitor<Type, Void>() {
              @Override
              public Type visitMethodInvocation(MethodInvocationTree tree, Void unused) {
                int idx = tree.getArguments().indexOf(conditionalExpression);
                if (idx == -1) {
                  return null;
                }
                MethodSymbol sym = ASTHelpers.getSymbol(tree);
                if (sym.getParameters().size() <= idx) {
                  Preconditions.checkState(sym.isVarArgs());
                  idx = sym.getParameters().size() - 1;
                }
                Type type = sym.getParameters().get(idx).asType();
                if (sym.isVarArgs() && idx == sym.getParameters().size() - 1) {
                  type = state.getTypes().elemtype(type);
                }
                return type;
              }

              @Override
              public Type visitVariable(VariableTree tree, Void unused) {
                return ASTHelpers.getType(tree);
              }

              @Override
              public Type visitBinary(BinaryTree tree, Void unused) {
                return ASTHelpers.getType(
                    ASTHelpers.stripParentheses(tree.getLeftOperand()).equals(conditionalExpression)
                        ? tree.getRightOperand()
                        : tree.getLeftOperand());
              }
            },
            null);
    if (type == null || !type.isPrimitive()) {
      return NO_MATCH;
    }
    return describeMatch(conditionalExpression);
  }
}
