/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.enclosingMethod;
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/**
 * Checks that {@link AsyncFunction} implementations do not directly {@code return null}.
 */
@BugPattern(name = "AsyncFunctionReturnsNull",
    summary = "AsyncFunction should not return a null Future, only a Future whose result is null.",
    explanation = "Methods like Futures.transformAsync and Futures.catchingAsync will throw a "
        + "NullPointerException if the provided AsyncFunction returns a null Future. To produce a "
        + "Future with an output of null, instead return immediateFuture(null).",
    category = GUAVA, severity = ERROR, maturity = EXPERIMENTAL)
public final class AsyncFunctionReturnsNull extends BugChecker implements ReturnTreeMatcher {
  @Override
  public Description matchReturn(ReturnTree tree, VisitorState state) {
    if (enclosingMethod(ASYNC_FUNCTION_APPLY_MATCHER).matches(tree, state)
        && tree.getExpression().getKind() == NULL_LITERAL) {
      return describeMatch(tree, SuggestedFix.builder()
          .replace(tree.getExpression(), "immediateFuture(null)")
          .addStaticImport(Futures.class.getName() + ".immediateFuture")
          .build());
    }
    return NO_MATCH;
  }

  private static final Matcher<MethodTree> ASYNC_FUNCTION_APPLY_MATCHER =
      overridesMethodOfClass(AsyncFunction.class);

  private static Matcher<MethodTree> overridesMethodOfClass(final Class<?> clazz) {
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree tree, VisitorState state) {
        MethodSymbol symbol = getSymbol(tree);
        if (symbol == null) {
          return false;
        }
        for (MethodSymbol superMethod : findSuperMethods(symbol, state.getTypes())) {
          if (superMethod.owner != null
              && superMethod.owner.getQualifiedName().contentEquals(clazz.getName())) {
            return true;
          }
        }
        return false;
      }
    };
  }
}
