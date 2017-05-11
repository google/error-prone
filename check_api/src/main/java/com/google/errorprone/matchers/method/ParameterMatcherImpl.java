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
import com.google.errorprone.matchers.method.MethodMatchers.ParameterMatcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;

/** Matches on a method's formal parameters. */
public class ParameterMatcherImpl extends AbstractChainedMatcher<MatchState, MatchState>
    implements ParameterMatcher {

  private final ImmutableList<Supplier<Type>> expected;

  ParameterMatcherImpl(
      AbstractSimpleMatcher<MatchState> baseMatcher, ImmutableList<Supplier<Type>> parameterTypes) {
    super(baseMatcher);
    this.expected = parameterTypes;
  }

  @Override
  protected Optional<MatchState> matchResult(
      ExpressionTree item, MatchState info, VisitorState state) {
    ImmutableList<Type> actual = ImmutableList.copyOf(info.paramTypes());
    if (actual.size() != expected.size()) {
      return Optional.absent();
    }
    for (int i = 0; i < actual.size(); ++i) {
      if (!ASTHelpers.isSameType(actual.get(i), expected.get(i).get(state), state)) {
        return Optional.absent();
      }
    }
    return Optional.of(info);
  }
}
