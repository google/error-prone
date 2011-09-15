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
 * Wraps another matcher and holds the reference to the matched AST node if it matches.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class CapturingMatcher<T extends Tree> extends Matcher<T> {
  private final Matcher<T> matcher;
  private final TreeHolder holder;

  public CapturingMatcher(Matcher<T> matcher, TreeHolder holder) {
    this.matcher = matcher;
    this.holder = holder;
  }

  @Override public boolean matches(T item, VisitorState state) {
    boolean matches = matcher.matches(item, state);
    if (matches) {
      holder.set(item);
    }
    return matches;
  }

  /**
   * A substitute for pass-by-reference, allowing the Tree field above to act as a return value.
   */
  public static class TreeHolder {
    private Tree value;

    public TreeHolder(Tree value) {
      this.value = value;
    }

    public TreeHolder() {
    }

    public void set(Tree value) {
      this.value = value;
    }

    public Tree get() {
      return value;
    }
  }
}
