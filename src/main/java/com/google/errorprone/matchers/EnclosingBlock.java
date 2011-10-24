/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

/**
 * Adapts a block matcher to match against the current enclosing block of whatever Tree last
 * set a TreePath into the state.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class EnclosingBlock<T extends Tree> implements Matcher<T> {
  private Matcher<BlockTree> matcher;

  public EnclosingBlock(Matcher<BlockTree> matcher) {
    this.matcher = matcher;
  }

  @Override
  public boolean matches(T unused, VisitorState state) {
    TreePath enclosingBlockPath = state.getPath();
    while (!(enclosingBlockPath.getLeaf() instanceof BlockTree)) {
      enclosingBlockPath = enclosingBlockPath.getParentPath();
    }
    BlockTree enclosingBlock = (BlockTree) enclosingBlockPath.getLeaf();
    return matcher.matches(enclosingBlock, state);
  }
}
