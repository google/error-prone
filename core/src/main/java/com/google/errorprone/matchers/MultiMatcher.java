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

  /**
   * This method is only valid to call after calling {@link #matches}. If that call returned true,
   * this method will return all nodes that matched (which could be empty if the multi matcher had
   * no nodes to process, so the child nodes vacuously matched the matcher). If that call returned
   * false (i.e.: this multimatcher did not match the matcher), this function will return an empty
   * list.
   *
   * @return all the child nodes that matched the matcher.
   * @throws IllegalStateException if {@link #matches} wasn't called beforehand.
   */
  List<N> getMatchingNodes();
}
