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
import java.util.List;

/**
 * A matcher for the next statement following a given statement.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class NextStatement<T extends StatementTree> implements Matcher<T> {
  private Matcher<StatementTree> matcher;

  public NextStatement(Matcher<StatementTree> matcher) {
    this.matcher = matcher;
  }

  @Override
  public boolean matches(T statement, VisitorState state) {
    List<? extends StatementTree> blockStatements =
        state.findEnclosing(BlockTree.class).getStatements();
    int statementIndex = blockStatements.indexOf(statement);

    if (statementIndex == -1) {
      // The block wrapping us doesn't contain this statement, e.g.:
      // { if (foo) return a; }
      // return a; isn't contained directly in a BlockTree :-|
      return false;
    }

    // find next statement
    statementIndex++;
    StatementTree nextStmt = null;
    if (statementIndex < blockStatements.size()) {
      nextStmt = blockStatements.get(statementIndex);
    }

    // TODO(glorioso): return false always instead of allowing the matcher to fail to match null?
    return matcher.matches(nextStmt, state);
  }
}
