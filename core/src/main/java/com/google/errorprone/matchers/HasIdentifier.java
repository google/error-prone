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

import static com.google.errorprone.matchers.MultiMatcher.MatchType.ALL;
import static com.google.errorprone.matchers.MultiMatcher.MatchType.ANY;

import com.google.errorprone.VisitorState;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.util.TreeScanner;

/**
 * Matches if the given matcher matches all of/any of the identifiers under this syntax tree.
 *
 * @author alexloh@google.com (Alex Loh)
 */
public class HasIdentifier extends MultiMatcher<Tree, IdentifierTree> {

  public HasIdentifier(MatchType matchType, Matcher<IdentifierTree> nodeMatcher) {
    super(matchType, nodeMatcher);
  }

  @Override
  public boolean matches(Tree tree, VisitorState state) {
    Boolean matches = tree.accept(new HasIdentifierScanner(matchType, nodeMatcher), null);
    return matches != null && matches;
  }

  /**
   * AST Visitor that matches identifiers in a Tree
   */
  private static class HasIdentifierScanner extends TreeScanner<Boolean, Void> {

    private Matcher<IdentifierTree> idMatcher;
    private MatchType matchType;

    public HasIdentifierScanner(MatchType matchType, Matcher<IdentifierTree> idMatcher) {
      this.matchType = matchType;
      this.idMatcher = idMatcher; 
    }

    @Override
    public Boolean visitIdentifier(IdentifierTree node, Void v) {
      return idMatcher.matches(node, null);
    }

    @Override
    public Boolean reduce(Boolean r1, Boolean r2) {
      if (r1 == null) {
        return r2;
      } else if (r2 == null) {
        return r1;
      } else { 
        if (matchType == ANY) {
          return r1 || r2;
        } else if (matchType == ALL) { 
          return r1 && r2;
        }
      }
      return null;
    }

    @Override
    public Boolean visitClass(ClassTree node, Void v) {
      Boolean res = super.visitClass(node, v);
      if (res == null) {
        if (matchType == ANY) {
          return false;
        } else if (matchType == ALL) { 
          return true;
        }
      }
      return res;
    }
  }
}
