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

package com.google.errorprone.matchers.method;

import com.google.common.base.Optional;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.method.MethodMatchers.ConstructorClassMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.ConstructorMatcher;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

/** Matches constructors, allows refinement on class type. */
public class ConstructorMatcherImpl extends AbstractSimpleMatcher<MatchState>
    implements ConstructorMatcher {

  @Override
  protected Optional<MatchState> matchResult(ExpressionTree tree, VisitorState state) {
    MethodSymbol sym = getConstructor(tree);
    if (sym == null) {
      return Optional.absent();
    }
    return Optional.of(MatchState.create(sym.owner.type, sym));
  }

  private static MethodSymbol getConstructor(ExpressionTree tree) {
    switch (tree.getKind()) {
      case NEW_CLASS:
      case METHOD_INVOCATION:
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
    return method;
  }

  @Override
  public ConstructorClassMatcher forClass(String className) {
    return new ConstructorClassMatcherImpl(this, TypePredicates.isExactType(className));
  }

  @Override
  public ConstructorClassMatcher forClass(Supplier<Type> classType) {
    return new ConstructorClassMatcherImpl(this, TypePredicates.isExactType(classType));
  }
}
