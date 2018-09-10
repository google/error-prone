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

package com.google.errorprone.matchers.method;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.method.MethodMatchers.MethodClassMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.MethodNameMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.MethodSignatureMatcher;
import com.google.errorprone.matchers.method.MethodNameMatcherImpl.Regex;
import com.google.errorprone.predicates.TypePredicate;
import com.sun.source.tree.ExpressionTree;
import java.util.regex.Pattern;

/** Matches on the method's class type, and allows refinement on method name or signature. */
class MethodClassMatcherImpl extends AbstractChainedMatcher<MatchState, MatchState>
    implements MethodClassMatcher {

  private final TypePredicate predicate;

  MethodClassMatcherImpl(AbstractSimpleMatcher<MatchState> baseMatcher, TypePredicate predicate) {
    super(baseMatcher);
    this.predicate = predicate;
  }

  @Override
  protected Optional<MatchState> matchResult(
      ExpressionTree item, MatchState method, VisitorState state) {
    return predicate.apply(method.ownerType(), state)
        ? Optional.of(method)
        : Optional.<MatchState>absent();
  }

  @Override
  public MethodNameMatcher named(String name) {
    return new MethodNameMatcherImpl.Exact(this, name);
  }

  @Override
  public MethodNameMatcher namedAnyOf(String... names) {
    return namedAnyOf(ImmutableList.copyOf(names));
  }

  @Override
  public MethodNameMatcher namedAnyOf(Iterable<String> names) {
    return new MethodNameMatcherImpl.AnyOf(this, names);
  }

  @Override
  public MethodNameMatcher withAnyName() {
    return new MethodNameMatcherImpl.Any(this);
  }

  @Override
  public MethodSignatureMatcher withSignature(String signature) {
    return new MethodSignatureMatcherImpl(this, signature);
  }

  @Override
  public MethodNameMatcher withNameMatching(Pattern pattern) {
    return new Regex(this, pattern);
  }
}
