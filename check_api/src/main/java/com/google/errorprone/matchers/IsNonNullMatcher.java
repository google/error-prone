/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.matchers;

import static com.google.errorprone.matchers.Matchers.isNonNullUsingDataflow;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

/**
 * Matches expressions that can be statically determined to be non-null. The matcher should have few
 * if any false positives but has many, many false negatives.
 */
public final class IsNonNullMatcher implements Matcher<ExpressionTree> {
  @Override
  public boolean matches(ExpressionTree tree, VisitorState state) {
    return isNonNullUsingDataflow().matches(tree, state);
  }
}
