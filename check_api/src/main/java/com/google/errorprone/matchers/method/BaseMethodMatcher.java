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
package com.google.errorprone.matchers.method;

import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import org.checkerframework.checker.nullness.qual.Nullable;

interface BaseMethodMatcher {
  @Nullable
  MatchState match(ExpressionTree tree);

  BaseMethodMatcher METHOD =
      tree -> {
        Symbol sym = ASTHelpers.getSymbol(tree);
        if (!(sym instanceof MethodSymbol)) {
          return null;
        }
        if (tree instanceof NewClassTree) {
          // Don't match constructors as they are neither static nor instance methods.
          return null;
        }
        if (tree instanceof MethodInvocationTree) {
          tree = ((MethodInvocationTree) tree).getMethodSelect();
        }
        return MethodMatchState.create(tree, (MethodSymbol) sym);
      };

  BaseMethodMatcher CONSTRUCTOR =
      tree -> {
        switch (tree.getKind()) {
          case NEW_CLASS:
          case METHOD_INVOCATION:
          case MEMBER_REFERENCE:
            break;
          default:
            return null;
        }
        Symbol sym = ASTHelpers.getSymbol(tree);
        if (!(sym instanceof MethodSymbol)) {
          return null;
        }
        MethodSymbol method = (MethodSymbol) sym;
        if (!method.isConstructor()) {
          return null;
        }
        return ConstructorMatchState.create(method);
      };
}
