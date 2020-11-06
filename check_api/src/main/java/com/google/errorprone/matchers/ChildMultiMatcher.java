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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.ForOverride;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.List;

/**
 * A {@link MultiMatcher} that applies a matcher across multiple children of a single ancestor node.
 * Configurable to return true if any of, all of, or the last node matches. In the any or last of
 * cases, provides access to the node that matched.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 * @param <T> the type of the node to match on
 * @param <N> the type of the subnode that the given matcher should match
 */
public abstract class ChildMultiMatcher<T extends Tree, N extends Tree>
    implements MultiMatcher<T, N> {

  public enum MatchType {
    /**
     * Matches if all of the child elements match the matcher. If the parent element has no child
     * elements, this matcher returns true.
     */
    ALL,
    /**
     * Matches if at least one of the child elements match the matcher. If the parent element has no
     * child elements, this matcher returns false.
     */
    AT_LEAST_ONE,
    /**
     * Matches if the last child element matches the matcher, regardless of whether or not any of
     * the other child elements would match the matcher. If the parent element has no child
     * elements, this matcher returns false.
     */
    LAST
  }

  @AutoValue
  abstract static class Matchable<T extends Tree> {
    public abstract T tree();

    public abstract VisitorState state();

    public static <T extends Tree> Matchable<T> create(T tree, VisitorState state) {
      return new AutoValue_ChildMultiMatcher_Matchable<>(tree, state);
    }
  }

  @AutoValue
  abstract static class MatchResult<T extends Tree> {
    public abstract ImmutableList<T> matchingNodes();

    public abstract boolean matches();

    public static <T extends Tree> MatchResult<T> none() {
      return create(ImmutableList.<T>of(), /* matches= */ false);
    }

    public static <T extends Tree> MatchResult<T> match(T matchingNode) {
      return create(ImmutableList.of(matchingNode), /* matches= */ true);
    }

    public static <T extends Tree> MatchResult<T> match(ImmutableList<T> matchingNodes) {
      return create(matchingNodes, /* matches= */ true);
    }

    private static <T extends Tree> MatchResult<T> create(List<T> matchingNode, boolean matches) {
      return new AutoValue_ChildMultiMatcher_MatchResult<>(
          ImmutableList.copyOf(matchingNode), matches);
    }
  }

  /**
   * A matcher that operates over a list of nodes, each of which includes an AST node and a
   * VisitorState with a TreePath for the given node.
   */
  private abstract static class ListMatcher<N extends Tree> {
    abstract MatchResult<N> matches(List<Matchable<N>> matchables, Matcher<N> nodeMatcher);

    public static <N extends Tree> ListMatcher<N> create(MatchType matchType) {
      switch (matchType) {
        case ALL:
          return new AllMatcher<>();
        case AT_LEAST_ONE:
          return new AtLeastOneMatcher<>();
        case LAST:
          return new LastMatcher<>();
      }
      throw new AssertionError("Unexpected match type: " + matchType);
    }
  }

  /** A matcher that returns true if all nodes in the list match. */
  private static class AllMatcher<N extends Tree> extends ListMatcher<N> {
    @Override
    public MatchResult<N> matches(List<Matchable<N>> matchables, Matcher<N> nodeMatcher) {
      ImmutableList.Builder<N> matchingTrees = ImmutableList.builder();
      for (Matchable<N> matchable : matchables) {
        if (!nodeMatcher.matches(matchable.tree(), matchable.state())) {
          return MatchResult.none();
        }
        matchingTrees.add(matchable.tree());
      }
      return MatchResult.match(matchingTrees.build());
    }
  }

  /** A matcher that returns true if at least one node in the list matches. */
  private static class AtLeastOneMatcher<N extends Tree> extends ListMatcher<N> {
    @Override
    public MatchResult<N> matches(List<Matchable<N>> matchables, Matcher<N> nodeMatcher) {
      ImmutableList.Builder<N> matchingTrees = ImmutableList.builder();
      for (Matchable<N> matchable : matchables) {
        if (nodeMatcher.matches(matchable.tree(), matchable.state())) {
          matchingTrees.add(matchable.tree());
        }
      }
      ImmutableList<N> allTheTrees = matchingTrees.build();
      return allTheTrees.isEmpty() ? MatchResult.<N>none() : MatchResult.match(allTheTrees);
    }
  }

  /** A matcher that returns true if the last node in the list matches. */
  private static class LastMatcher<N extends Tree> extends ListMatcher<N> {
    @Override
    public MatchResult<N> matches(List<Matchable<N>> matchables, Matcher<N> nodeMatcher) {
      if (matchables.isEmpty()) {
        return MatchResult.none();
      }
      Matchable<N> last = Iterables.getLast(matchables);
      return nodeMatcher.matches(last.tree(), last.state())
          ? MatchResult.match(last.tree())
          : MatchResult.<N>none();
    }
  }

  /** The matcher to apply to the subnodes in question. */
  protected final Matcher<N> nodeMatcher;

  private final ListMatcher<N> listMatcher;

  public ChildMultiMatcher(MatchType matchType, Matcher<N> nodeMatcher) {
    this.nodeMatcher = nodeMatcher;
    this.listMatcher = ListMatcher.create(matchType);
  }

  @Override
  public boolean matches(T tree, VisitorState state) {
    return multiMatchResult(tree, state).matches();
  }

  @Override
  public MultiMatchResult<N> multiMatchResult(T tree, VisitorState state) {
    ImmutableList.Builder<Matchable<N>> result = ImmutableList.builder();
    for (N subnode : getChildNodes(tree, state)) {
      TreePath newPath = new TreePath(state.getPath(), subnode);
      result.add(Matchable.create(subnode, state.withPath(newPath)));
    }
    MatchResult<N> matchResult = listMatcher.matches(result.build(), nodeMatcher);
    return MultiMatchResult.create(matchResult.matches(), matchResult.matchingNodes());
  }

  /**
   * Returns the set of child nodes to match. The nodes must be immediate children of the current
   * node to ensure the TreePath calculation is correct. MultiMatchers with other requirements
   * should not subclass ChildMultiMatcher.
   */
  @ForOverride
  protected abstract Iterable<? extends N> getChildNodes(T tree, VisitorState state);
}
