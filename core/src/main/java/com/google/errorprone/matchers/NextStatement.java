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
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;

import java.util.List;

/**
 * A matcher for the next statement following a given statement.
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class NextStatement<T extends StatementTree> implements Matcher<T> {
  private Matcher<StatementTree> matcher;

  public NextStatement(Matcher<StatementTree> matcher) {
    this.matcher = matcher;
  }

  @Override
  public boolean matches(T stmt, VisitorState state) {
    // TODO(user): should re-use Enclosing.BlockOrCase
    // find enclosing block
    TreePath path = state.getPath();
    Tree prev = null;
    Tree curr = path.getLeaf();     // initialized to curr node (if stmt)
    boolean found = false;
    while (path != null) {
      prev = curr;
      path = path.getParentPath();
      curr = path.getLeaf();
      if (curr.getKind() == Kind.BLOCK) {
        found = true;
        break;
      }
    }
    assert(found);      // should always find an enclosing block
    BlockTree block = (BlockTree)curr;

    // find next statement
    List<? extends StatementTree> stmts = block.getStatements();
    int ifStmtIdx = stmts.indexOf(prev);
    StatementTree nextStmt = null;
    if (ifStmtIdx < stmts.size() - 1) {  // TODO(user): off by one?
      nextStmt = stmts.get(ifStmtIdx + 1);
    }

    return matcher.matches(nextStmt, state);
  }
}
