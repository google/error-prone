/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isVoidType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/**
 * Non-abstract instance methods named {@code self()} that return the enclosing class must always
 * {@code return this}.
 */
@BugPattern(
    summary =
        "Non-abstract instance methods named 'self()' that return the enclosing class must always"
            + " 'return this'",
    severity = WARNING)
public final class SelfAlwaysReturnsThis extends BugChecker implements MethodTreeMatcher {
  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    MethodSymbol methodSymbol = getSymbol(methodTree);

    // The method must:
    // * not be a constructor
    // * be named `self`
    // * have no params
    // * be an instance method (not static)
    // * have a body (not abstract)
    if (methodSymbol.isConstructor()
        || !methodSymbol.getSimpleName().contentEquals("self")
        || !methodSymbol.getParameters().isEmpty()
        || methodSymbol.isStatic()
        || methodTree.getBody() == null) {
      return NO_MATCH;
    }

    // * not have a void (or Void) return type
    Tree returnType = methodTree.getReturnType();
    if (isVoidType(getType(returnType), state)) {
      return NO_MATCH;
    }

    // * have the same return type as the enclosing type
    if (!isSameType(getType(returnType), enclosingClass(methodSymbol).type, state)) {
      return NO_MATCH;
    }

    // TODO(kak): we should probably re-used the TreePathScanner from CanIgnoreReturnValueSuggester

    // * have a body that is exactly 1 statement
    if (methodTree.getBody().getStatements().size() == 1) {

      // * the 1 statement is "return this;" or "return (T) this;"
      StatementTree statement = methodTree.getBody().getStatements().get(0);
      if (statement instanceof ReturnTree) {
        ExpressionTree returnExpression = ((ReturnTree) statement).getExpression();

        if (maybeCastThis(returnExpression)) {
          return NO_MATCH;
        }
      }
    }

    // * or have a body that is exactly 2 statement
    if (methodTree.getBody().getStatements().size() == 2) {
      // * the 1st statement is an assignment (e.g., Builder self = (Builder) this;)
      StatementTree firstStatement = methodTree.getBody().getStatements().get(0);
      StatementTree secondStatement = methodTree.getBody().getStatements().get(1);
      if (firstStatement instanceof VariableTree && secondStatement instanceof ReturnTree) {
        if (maybeCastThis(((VariableTree) firstStatement).getInitializer())
            && getSymbol(firstStatement)
                .equals(getSymbol(((ReturnTree) secondStatement).getExpression()))) {
          return NO_MATCH;
        }
      }
    }

    return describeMatch(
        methodTree, SuggestedFix.replace(methodTree.getBody(), "{ return this; }"));
  }

  private static boolean maybeCastThis(Tree tree) {
    return firstNonNull(
        new SimpleTreeVisitor<Boolean, Void>() {
          @Override
          public Boolean visitTypeCast(TypeCastTree tree, Void unused) {
            return visit(tree.getExpression(), null);
          }

          @Override
          public Boolean visitIdentifier(IdentifierTree tree, Void unused) {
            return isThis(tree);
          }
        }.visit(tree, null),
        false);
  }

  /** Returns whether the given {@link ExpressionTree} is exactly {@code this}. */
  private static boolean isThis(ExpressionTree expression) {
    if (expression instanceof IdentifierTree) {
      return ((IdentifierTree) expression).getName().contentEquals("this");
    }
    return false;
  }
}
