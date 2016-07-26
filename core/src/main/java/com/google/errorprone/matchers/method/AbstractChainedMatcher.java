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
import com.google.errorprone.annotations.ForOverride;
import com.sun.source.tree.ExpressionTree;

/** Super-type for matchers that compose other matchers. */
abstract class AbstractChainedMatcher<A, B> extends AbstractSimpleMatcher<B> {
  private final AbstractSimpleMatcher<A> baseMatcher;

  AbstractChainedMatcher(AbstractSimpleMatcher<A> baseMatcher) {
    this.baseMatcher = baseMatcher;
  }

  @ForOverride
  protected abstract Optional<B> matchResult(ExpressionTree item, A baseResult, VisitorState state);

  @Override
  protected final Optional<B> matchResult(ExpressionTree item, VisitorState state) {
    Optional<A> baseResult = baseMatcher.matchResult(item, state);
    return baseResult.isPresent()
        ? matchResult(item, baseResult.get(), state)
        : Optional.<B>absent();
  }
}
