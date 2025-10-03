/*
 * Copyright 2015 The Error Prone Authors.
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
import com.sun.source.util.TreeScanner;

/**
 * A matcher that recursively inspects a tree, applying the given matcher to all levels of each tree
 * and returning {@code true} if any match is found.
 *
 * <p>This matcher may be slow. Please avoid using it if there is any other way to implement your
 * check.
 */
public class Contains implements Matcher<Tree> {

  private final Matcher<Tree> matcher;

  public Contains(Matcher<Tree> matcher) {
    this.matcher = matcher;
  }

  @Override
  public boolean matches(Tree tree, VisitorState state) {
    FirstMatchingScanner scanner = new FirstMatchingScanner(state);
    Boolean matchFound = tree.accept(scanner, /* data= */ false);
    return matchFound != null && matchFound;
  }

  private class FirstMatchingScanner extends TreeScanner<Boolean, Boolean> {

    private final VisitorState state;

    FirstMatchingScanner(VisitorState state) {
      this.state = state;
    }

    @Override
    public Boolean scan(Tree tree, Boolean matchFound) {
      if (matchFound) {
        return true;
      }
      if (matcher.matches(tree, state)) {
        return true;
      }
      return super.scan(tree, false);
    }

    @Override
    public Boolean reduce(Boolean left, Boolean right) {
      return (left != null && left) || (right != null && right);
    }
  }
}
