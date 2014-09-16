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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;

import java.util.List;

/**
 * Adapt matchers to match against a parent node of a given type. For example, match a node if the
 * enclosing class matches a predicate.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class Enclosing {
  private Enclosing() {}

  private static abstract class EnclosingMatcher<T extends Tree, U> implements Matcher<U> {
    protected final Matcher<T> matcher;
    protected final java.lang.Class<T> clazz;

    protected EnclosingMatcher(Matcher<T> matcher, java.lang.Class<T> clazz) {
      this.matcher = matcher;
      this.clazz = clazz;
    }

    @Override
    public boolean matches(U unused, VisitorState state) {
      T enclosing = state.findEnclosing(clazz);
      // No match if there is no enclosing element to match against
      if (enclosing == null) {
        return false;
      }
      return matcher.matches(enclosing, state);
    }
  }

  public static class Block<T extends Tree> extends EnclosingMatcher<BlockTree, T> {
    public Block(Matcher<BlockTree> matcher) {
      super(matcher, BlockTree.class);
    }
  }

  public static class Class<T extends Tree> extends EnclosingMatcher<ClassTree, T> {
    public Class(Matcher<ClassTree> matcher) {
      super(matcher, ClassTree.class);
    }
  }

  public static class Method<T extends Tree> extends EnclosingMatcher<MethodTree, T> {
    public Method(Matcher<MethodTree> matcher) {
      super(matcher, MethodTree.class);
    }
  }

  public static class BlockOrCase<T extends Tree> implements Matcher<T> {
    private final Matcher<List<StatementTree>> matcher;

    public BlockOrCase(Matcher<List<StatementTree>> matcher) {
      this.matcher = matcher;
    }

    @Override
    public boolean matches(T unused, VisitorState state) {
      Tree enclosing = state.findEnclosing(CaseTree.class, BlockTree.class);
      if (enclosing == null) {
        return false;
      }
      final List<? extends StatementTree> statements;
      if (enclosing instanceof BlockTree) {
        statements = ((BlockTree)enclosing).getStatements();
      } else if (enclosing instanceof CaseTree) {
        statements = ((CaseTree)enclosing).getStatements();
      } else {
        // findEnclosing given two types must return something of one of those types
        throw new IllegalStateException("enclosing tree not a BlockTree or CaseTree");
      }
      return matcher.matches(ImmutableList.copyOf(statements), state);
    }
  }
}
