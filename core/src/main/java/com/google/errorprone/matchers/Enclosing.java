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
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

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
      return matcher.matches(findEnclosing(BlockTree.class, state), state);
    }
  }

  public static class Class<T extends Tree> implements Matcher<T> {
    private Matcher<ClassTree> matcher;

    public Class(Matcher<ClassTree> matcher) {
      this.matcher = matcher;
    }

    @Override
    public boolean matches(T unused, VisitorState state) {
      return matcher.matches(findEnclosing(ClassTree.class, state), state);
    }
  }

  public static class Method<T extends Tree> implements Matcher<T> {
    private Matcher<MethodTree> matcher;

    public Method(Matcher<MethodTree> matcher) {
      this.matcher = matcher;
    }
    @Override
    public boolean matches(T unused, VisitorState state) {
      return matcher.matches(findEnclosing(MethodTree.class, state), state);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T extends Tree> T findEnclosing(java.lang.Class<T> clazz, VisitorState state) {
    TreePath enclosingPath = state.getPath();
    while (!(clazz.isAssignableFrom(enclosingPath.getLeaf().getClass()))) {
      enclosingPath = enclosingPath.getParentPath();
    }
    return (T) enclosingPath.getLeaf();
  }
}
