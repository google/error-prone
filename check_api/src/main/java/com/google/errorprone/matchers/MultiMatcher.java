/*
 * Copyright 2013 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.Tree;
import java.util.List;

/**
 * An matcher that applies a single matcher across multiple tree nodes.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 * @param <T> the type of the node to match on
 * @param <N> the type of the subnode that the given matcher should match
 */
public interface MultiMatcher<T extends Tree, N extends Tree> extends Matcher<T> {

  /** Attempt to match the given node, and return the associated subnodes that matched. */
  MultiMatchResult<N> multiMatchResult(T tree, VisitorState vs);

  /**
   * A result from the call of {@link MultiMatcher#multiMatchResult(Tree, VisitorState)}, containing
   * information about whether it matched, and if so, what nodes matched.
   */
  @AutoValue
  abstract class MultiMatchResult<N extends Tree> {
    MultiMatchResult() {}
    /** True if the MultiMatcher matched the nodes expected. */
    public abstract boolean matches();
    /**
     * The list of nodes which matched the MultiMatcher's expectations (could be empty if the match
     * type was ALL and there were no child nodes). Only sensical if {@link #matches()} is true.
     */
    public abstract ImmutableList<N> matchingNodes();

    public final N onlyMatchingNode() {
      return getOnlyElement(matchingNodes());
    }

    static <N extends Tree> MultiMatchResult<N> create(boolean matches, List<N> matchingNodes) {
      return new AutoValue_MultiMatcher_MultiMatchResult<>(
          matches, ImmutableList.copyOf(matchingNodes));
    }
  }
}
