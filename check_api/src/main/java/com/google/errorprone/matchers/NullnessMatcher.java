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

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.util.TreePath;

/** Matches an expression based on the result of the nullness dataflow analysis. */
public class NullnessMatcher implements Matcher<ExpressionTree> {
  private final Nullness expectedNullnessValue;

  public NullnessMatcher(Nullness expectedNullnessValue) {
    this.expectedNullnessValue = expectedNullnessValue;
  }

  @Override
  public boolean matches(ExpressionTree expr, VisitorState state) {
    TreePath exprPath = new TreePath(state.getPath(), expr);
    return state.getNullnessAnalysis().getNullness(exprPath, state.context)
        == expectedNullnessValue;
  }
}
