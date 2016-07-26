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
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;

/** Super-type for base (non-chained) matchers. */
abstract class AbstractSimpleMatcher<T> implements Matcher<ExpressionTree> {
  protected abstract Optional<T> matchResult(ExpressionTree item, VisitorState state);

  @Override
  public final boolean matches(ExpressionTree tree, VisitorState state) {
    return matchResult(tree, state).isPresent();
  }
}
