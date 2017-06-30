/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.common.collect.Iterables;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.StatementTree;

/** A matcher that returns true if the statement is the final statement in the block. */
public class IsLastStatementInBlock<T extends StatementTree> implements Matcher<T> {

  @Override
  public boolean matches(T statement, VisitorState state) {
    BlockTree block = state.findEnclosing(BlockTree.class);

    return block != null && Iterables.getLast(block.getStatements()).equals(statement);
  }
}
