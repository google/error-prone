/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.matchers.Matchers.methodReturnsNonNull;

import com.google.errorprone.JDKCompatible;
import com.google.errorprone.VisitorState;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.VariableTree;

/**
 * Matches an expression if it can be determined as definitely non-null.
 */
public class DefinitelyNonNull implements Matcher<ExpressionTree> {
  @Override
  public boolean matches(ExpressionTree tree, VisitorState state) {
    // TODO(user), move this into isDefinitelyNonNull
    if (methodReturnsNonNull().matches(tree, state)) {
      return true;
    }

    // TODO(user), move this into isDefinitelyNonNull
    if (tree instanceof VariableTree
        || tree instanceof IdentifierTree
        || tree instanceof LiteralTree) {
      return JDKCompatible.isDefinitelyNonNull(state.getPath(), state.context);
    }
    
    return false;
  }
}