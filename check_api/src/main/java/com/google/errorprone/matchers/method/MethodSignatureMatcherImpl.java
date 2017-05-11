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
import com.google.errorprone.matchers.method.MethodMatchers.MethodSignatureMatcher;
import com.sun.source.tree.ExpressionTree;

/** Matches on method signature. */
public class MethodSignatureMatcherImpl extends AbstractChainedMatcher<MatchState, MatchState>
    implements MethodSignatureMatcher {
  private final String methodName;

  MethodSignatureMatcherImpl(AbstractSimpleMatcher<MatchState> baseMatcher, String methodName) {
    super(baseMatcher);
    this.methodName = methodName;
  }

  @Override
  protected Optional<MatchState> matchResult(
      ExpressionTree item, MatchState method, VisitorState state) {
    // TODO(cushon): build a way to match signatures (including varargs ones!) that doesn't
    // rely on MethodSymbol#toString().
    boolean matches =
        method.sym().getSimpleName().contentEquals(methodName)
            || method.sym().toString().equals(methodName);
    return matches ? Optional.of(method) : Optional.<MatchState>absent();
  }
}
