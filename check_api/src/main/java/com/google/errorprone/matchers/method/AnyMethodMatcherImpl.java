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
import com.google.errorprone.matchers.method.MethodMatchers.AnyMethodMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.MethodClassMatcher;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.predicates.TypePredicates;
import com.sun.source.tree.ExpressionTree;

/** Matches instance or static methods, allows refinement on class type. */
class AnyMethodMatcherImpl extends MethodMatcher implements AnyMethodMatcher {

  @Override
  protected Optional<MatchState> matchResult(
      ExpressionTree method, MatchState baseResult, VisitorState state) {
    return Optional.of(baseResult);
  }

  @Override
  public MethodClassMatcher onClass(TypePredicate predicate) {
    return new MethodClassMatcherImpl(this, predicate);
  }

  @Override
  public MethodClassMatcherImpl anyClass() {
    return new MethodClassMatcherImpl(this, TypePredicates.anyType());
  }
}
