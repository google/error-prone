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

import com.google.errorprone.VisitorState;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;

import java.util.List;

/**
 * Adapt matchers to match against a parent node of a given type. For example, match a node if the
 * enclosing class matches a predicate.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class Enclosing {
  private Enclosing() {}

  public static class Block<T extends Tree> implements Matcher<T> {
    private final Matcher<BlockTree> matcher;

    public Block(Matcher<BlockTree> matcher) {
      this.matcher = matcher;
    }

    @Override
    public boolean matches(T unused, VisitorState state) {
      return matcher.matches(state.findEnclosing(BlockTree.class), state);
    }
  }

  public static class BlockOrCase<T extends Tree> implements Matcher<T> {
    private final Matcher<List<? extends StatementTree>> matcher;

    public BlockOrCase(Matcher<List<? extends StatementTree>> matcher) {
      this.matcher = matcher;
    }

    @Override
    public boolean matches(T unused, VisitorState state) {
      Tree enclosing = state.findEnclosing(CaseTree.class, BlockTree.class);
      final List<? extends StatementTree> statements;
      if (enclosing instanceof BlockTree) {
        statements = ((BlockTree)enclosing).getStatements();
      } else if (enclosing instanceof CaseTree) {
        statements = ((CaseTree)enclosing).getStatements();
      } else {
        // findEnclosing given two types must return something of one of those types
        throw new IllegalStateException("enclosing tree not a BlockTree or CaseTree");
      }
      return matcher.matches(statements, state);
    }
  }

  public static class Class<T extends Tree> implements Matcher<T> {
    private Matcher<ClassTree> matcher;

    public Class(Matcher<ClassTree> matcher) {
      this.matcher = matcher;
    }

    @Override
    public boolean matches(T unused, VisitorState state) {
      return matcher.matches(state.findEnclosing(ClassTree.class), state);
    }
  }

  public static class Method<T extends Tree> implements Matcher<T> {
    private Matcher<MethodTree> matcher;

    public Method(Matcher<MethodTree> matcher) {
      this.matcher = matcher;
    }
    @Override
    public boolean matches(T unused, VisitorState state) {
      return matcher.matches(state.findEnclosing(MethodTree.class), state);
    }
  }
}
