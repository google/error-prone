/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "This method always recurses, and will cause a StackOverflowError",
    severity = ERROR)
public class InfiniteRecursion extends BugChecker implements BugChecker.MethodTreeMatcher {
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (tree.getBody() == null || tree.getBody().getStatements().size() != 1) {
      return NO_MATCH;
    }
    Tree statement = getOnlyElement(tree.getBody().getStatements());
    MethodInvocationTree invocation =
        statement.accept(
            new SimpleTreeVisitor<MethodInvocationTree, Void>() {
              @Override
              public MethodInvocationTree visitExpressionStatement(
                  ExpressionStatementTree tree, Void unused) {
                return visit(tree.getExpression(), null);
              }

              @Override
              public MethodInvocationTree visitParenthesized(ParenthesizedTree tree, Void unused) {
                return visit(tree.getExpression(), null);
              }

              @Override
              public MethodInvocationTree visitReturn(ReturnTree tree, Void unused) {
                return visit(tree.getExpression(), null);
              }

              @Override
              public MethodInvocationTree visitTypeCast(TypeCastTree tree, Void unused) {
                return visit(tree.getExpression(), null);
              }

              @Override
              protected @Nullable MethodInvocationTree defaultAction(Tree tree, Void unused) {
                return tree instanceof MethodInvocationTree ? (MethodInvocationTree) tree : null;
              }
            },
            null);
    if (invocation == null) {
      return NO_MATCH;
    }
    ExpressionTree select = invocation.getMethodSelect();
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    if (!sym.equals(ASTHelpers.getSymbol(invocation))) {
      return NO_MATCH;
    }
    if (!sym.isStatic()) {
      switch (select.getKind()) {
        case IDENTIFIER:
          break;
        case MEMBER_SELECT:
          ExpressionTree receiver = ((MemberSelectTree) select).getExpression();
          if (receiver.getKind() != Kind.IDENTIFIER) {
            return NO_MATCH;
          }
          if (!((IdentifierTree) receiver).getName().contentEquals("this")) {
            return NO_MATCH;
          }
          break;
        default:
          return NO_MATCH;
      }
    }
    return describeMatch(statement);
  }
}
