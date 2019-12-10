/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "CacheLoaderNull",
    summary = "The result of CacheLoader#load must be non-null.",
    severity = WARNING)
public class CacheLoaderNull extends BugChecker implements MethodTreeMatcher {

  private static final Supplier<Type> CACHE_LOADER_TYPE =
      Suppliers.typeFromString("com.google.common.cache.CacheLoader");

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!tree.getName().contentEquals("load")) {
      return NO_MATCH;
    }
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    if (!ASTHelpers.isSubtype(sym.owner.asType(), CACHE_LOADER_TYPE.get(state), state)) {
      return NO_MATCH;
    }
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitLambdaExpression(LambdaExpressionTree tree, Void unused) {
        return null;
      }

      @Override
      public Void visitClass(ClassTree tree, Void unused) {
        return null;
      }

      @Override
      public Void visitReturn(ReturnTree tree, Void unused) {
        ExpressionTree expression = tree.getExpression();
        if (expression != null && expression.getKind() == Tree.Kind.NULL_LITERAL) {
          state.reportMatch(describeMatch(tree));
        }
        return super.visitReturn(tree, null);
      }
    }.scan(tree.getBody(), null);
    return NO_MATCH;
  }
}
