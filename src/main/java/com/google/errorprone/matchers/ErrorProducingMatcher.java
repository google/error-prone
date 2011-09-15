/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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
import com.sun.source.tree.Tree;

/**
 * A matcher which produces an error instead of just a boolean when the predicate matches.
 * @author alexeagle@google.com (Alex Eagle)
 */
public abstract class ErrorProducingMatcher<T extends Tree> extends Matcher<T> {
  /**
   *
   * @param t an AST node
   * @param state
   * @return an error if the node matches the predicate, otherwise null
   */
  public abstract AstError matchWithError(T t, VisitorState state);

  @Override
  public boolean matches(T t, VisitorState state) {
    return matchWithError(t, state) != null;
  }

  public static class AstError {
    public String message;
    public Tree match;

    public AstError(String message, Tree match) {
      this.message = message;
      this.match = match;
    }
  }
}
