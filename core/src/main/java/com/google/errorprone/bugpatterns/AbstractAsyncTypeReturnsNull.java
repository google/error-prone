/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.util.concurrent.Futures;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Optional;

/**
 * Superclass for checks that {@code AsyncCallable} and {@code AsyncFunction} implementations do not
 * directly {@code return null}.
 */
abstract class AbstractAsyncTypeReturnsNull extends AbstractMethodReturnsNull {

  AbstractAsyncTypeReturnsNull(Class<?> asyncClass) {
    super(overridesMethodOfClass(asyncClass));
  }

  @Override
  protected Optional<Fix> provideFix(ReturnTree tree) {
    return Optional.of(
        SuggestedFix.builder()
            .replace(tree.getExpression(), "immediateFuture(null)")
            .addStaticImport(Futures.class.getName() + ".immediateFuture")
            .build());
  }

  private static Matcher<MethodTree> overridesMethodOfClass(final Class<?> clazz) {
    checkNotNull(clazz);
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
