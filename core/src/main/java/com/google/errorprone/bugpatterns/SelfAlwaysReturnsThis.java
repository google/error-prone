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
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isVoidType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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

    // This TreePathScanner is mostly copied from CanIgnoreReturnValueSuggester
    AtomicBoolean allReturnThis = new AtomicBoolean(true);
    AtomicBoolean atLeastOneReturn = new AtomicBoolean(false);

    new TreePathScanner<Void, Void>() {
      private final Set<VarSymbol> thises = new HashSet<>();

      @Override
      public Void visitVariable(VariableTree variableTree, Void unused) {
        VarSymbol symbol = getSymbol(variableTree);
        if (isConsideredFinal(symbol) && maybeCastThis(variableTree.getInitializer())) {
          thises.add(symbol);
        }
        return super.visitVariable(variableTree, null);
      }

      @Override
      public Void visitReturn(ReturnTree returnTree, Void unused) {
        atLeastOneReturn.set(true);
        if (!isThis(returnTree.getExpression())) {
          allReturnThis.set(false);
          // once we've set allReturnThis to false, no need to descend further
          return null;
        }
        return super.visitReturn(returnTree, null);
      }

      /** Returns whether the given {@link ExpressionTree} is {@code this}. */
      private boolean isThis(ExpressionTree returnExpression) {
        return maybeCastThis(returnExpression) || thises.contains(getSymbol(returnExpression));
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
        // don't descend into lambdas
        return null;
      }

      @Override
      public Void visitNewClass(NewClassTree node, Void unused) {
        // don't descend into declarations of anonymous classes
        return null;
      }
    }.scan(state.getPath(), null);

    if (atLeastOneReturn.get() && allReturnThis.get()) {
      return NO_MATCH;
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
            return tree.getName().contentEquals("this");
          }
        }.visit(tree, null),
        false);
  }
}
