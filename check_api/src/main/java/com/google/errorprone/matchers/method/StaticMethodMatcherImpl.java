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
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.method.MethodMatchers.StaticMethodMatcher;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;

/** Matches static methods, allows refinement on class type. */
class StaticMethodMatcherImpl extends MethodMatcher implements StaticMethodMatcher {
  @Override
  protected Optional<MatchState> matchResult(
      ExpressionTree item, MatchState method, VisitorState state) {
    if (!method.sym().isStatic()) {
      return Optional.absent();
    }
    return Optional.of(method);
  }

  @Override
  public MethodClassMatcherImpl onClass(TypePredicate predicate) {
    return new MethodClassMatcherImpl(this, predicate);
  }

  @Override
  public MethodClassMatcherImpl onClass(String className) {
    return new MethodClassMatcherImpl(this, TypePredicates.isExactType(className));
  }

  @Override
  public MethodClassMatcherImpl onClassAny(Iterable<String> classNames) {
    return new MethodClassMatcherImpl(this, TypePredicates.isExactTypeAny(classNames));
  }

  @Override
  public MethodClassMatcherImpl onClassAny(String... classNames) {
    return onClassAny(ImmutableList.copyOf(classNames));
  }

  @Override
  public MethodClassMatcherImpl onClass(Supplier<Type> classType) {
    return new MethodClassMatcherImpl(this, TypePredicates.isExactType(classType));
  }

  @Override
  public MethodMatchers.MethodClassMatcher anyClass() {
    return new MethodClassMatcherImpl(this, TypePredicates.anyType());
  }
}
