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

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCNewClass;

/** Matches constructors, allows refinement on class type. */
public class ConstructorMatcherImpl extends AbstractSimpleMatcher<MatchState>
    implements ConstructorMatcher {

  @Override
  protected Optional<MatchState> matchResult(ExpressionTree tree, VisitorState state) {
    // TODO(eaftan): Don't catch NullPointerException. Need to do this right now
    // for internal use, but remember to remove later.
    try {
      if (!(tree instanceof JCNewClass)) {
        return Optional.absent();
      }
      JCNewClass newClass = (JCNewClass) tree;
      Type clazz = newClass.constructor.getEnclosingElement().type;
      if (!(newClass.constructor instanceof MethodSymbol)) {
        return Optional.absent();
      }
      MethodSymbol sym = (MethodSymbol) newClass.constructor;
      return Optional.of(MatchState.create(clazz, sym));
    } catch (NullPointerException e) {
      return Optional.absent();
    }
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
