/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

/**
 * An abstract class for matchers that applies a single matcher across multiple tree nodes.
 * Configurable to return true if any of or all of the tree nodes match.  In the any of case,
 * provides access to the node that matched.
 *
 * TODO(user): Currently this class is used to match a single matcher against multiple elements
 * under some root element.  It might make sense to refactor this into matcher types with different
 * semantics -- allOf, anyOf, match the nth element, etc.  Then the matchers that currently extend
 * this would instead take one of these as a parameter and use that to define how to do the
 * matching.  This would be more general and composable.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 * @param <T> the type of the node to match on
 * @param <N> the type of the subnode that the given matcher should match
 */
public abstract class MultiMatcher<T, N> implements Matcher<T> {

  public enum MatchType {
    ALL,
    ANY
  }

  /**
   * Whether to match all of or any of the nodes.
   */
  final MatchType matchType;

  /**
   * The matcher to apply to the subnodes in question.
   */
  final Matcher<N> nodeMatcher;

  /**
   * The matching node.  Only set when MatchType is ANY.
   */
  N matchingNode;

  public MultiMatcher(MatchType matchType, Matcher<N> nodeMatcher) {
    this.nodeMatcher = nodeMatcher;
    this.matchType = matchType;
  }

  /**
   * Returns the node that matched.  Node will be non-null.
   */
  public N getMatchingNode() {
    if (matchType == MatchType.ALL) {
      throw new IllegalStateException("getMatchingNode() makes no sense when matching all nodes");
    }
    if (matchingNode == null) {
      throw new IllegalStateException("No nodes matched");
    }
    return matchingNode;
  }
}
