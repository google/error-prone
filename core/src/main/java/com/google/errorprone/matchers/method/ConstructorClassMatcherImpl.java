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
import com.google.errorprone.matchers.method.MethodMatchers.ParameterMatcher;
import com.google.errorprone.predicates.TypePredicate;

import com.sun.source.tree.ExpressionTree;

import java.util.Arrays;

/** Matches on class type, allows refinement on parameters. */
class ConstructorClassMatcherImpl extends AbstractChainedMatcher<MatchState, MatchState>
    implements ConstructorClassMatcher {

  private final TypePredicate predicate;

  @Override
  protected Optional<MatchState> matchResult(ExpressionTree item, MatchState baseResult,
      VisitorState state) {
    if (predicate.apply(baseResult.ownerType(), state)) {
      return Optional.of(baseResult);
    }
    return Optional.absent();
  }

  public ConstructorClassMatcherImpl(ConstructorMatcherImpl baseMatcher,
      TypePredicate predicate) {
    super(baseMatcher);
    this.predicate = predicate;
  }

  @Override
  public ParameterMatcher withParameters(String... parameters) {
    return withParameters(Arrays.asList(parameters));
  }

  @Override
  public ParameterMatcher withParameters(Iterable<String> parameters) {
    return new ParameterMatcherImpl(this, parameters);
  }
}