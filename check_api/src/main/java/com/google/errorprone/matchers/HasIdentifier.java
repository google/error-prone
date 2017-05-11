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

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePathScanner;

/**
 * Matches if the given matcher matches all of the identifiers under this syntax tree.
 *
 * @author alexloh@google.com (Alex Loh)
 */
public class HasIdentifier implements Matcher<Tree> {

  private final Matcher<IdentifierTree> nodeMatcher;

  public HasIdentifier(Matcher<IdentifierTree> nodeMatcher) {
    this.nodeMatcher = nodeMatcher;
  }

  @Override
  public boolean matches(Tree tree, VisitorState state) {
    Boolean matches = new HasIdentifierScanner(state, nodeMatcher).scan(state.getPath(), null);
    return firstNonNull(matches, false);
  }

  /** AST Visitor that matches identifiers in a Tree */
  private static class HasIdentifierScanner extends TreePathScanner<Boolean, Void> {

    private Matcher<IdentifierTree> idMatcher;
    private VisitorState ancestorState;

    public HasIdentifierScanner(VisitorState ancestorState, Matcher<IdentifierTree> idMatcher) {
      this.ancestorState = ancestorState;
      this.idMatcher = idMatcher;
    }

    @Override
    public Boolean visitIdentifier(IdentifierTree node, Void v) {
      return idMatcher.matches(node, ancestorState.withPath(getCurrentPath()));
    }

    @Override
    public Boolean reduce(Boolean r1, Boolean r2) {
      return firstNonNull(r1, false) || firstNonNull(r2, false);
    }

    @Override
    public Boolean visitClass(ClassTree node, Void v) {
      return firstNonNull(super.visitClass(node, v), false);
    }
  }
}
