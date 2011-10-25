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

import com.google.errorprone.SuggestedFix;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

/**
 * Checks for the presence of an error, and gives a message and suggested fix for matches.
 * @author alexeagle@google.com (Alex Eagle)
 */
public abstract class ErrorChecker<T extends Tree> implements Matcher<T> {

  private VisitorState state;

  protected Symtab getSymbolTable() {
    return state.symtab;
  }

  protected TreePath getPath() {
    return state.getPath();
  }

  @Override
  public boolean matches(T t, VisitorState state) {
    this.state = state;
    return matcher().matches(t, state);
  }

  public abstract Matcher<T> matcher();

  public AstError check(T tree, VisitorState state) {
    if (matches(tree, state)) {
      return produceError(tree, state);
    }
    return null;
  }

  public static class Position {
    public final int start;
    public final int end;

    public Position(int start, int end) {
      this.start = start;
      this.end = end;
    }
  }

  protected Position getSourcePosition(Tree tree) {
    DiagnosticPosition pos = ((JCTree) tree).pos();
    return new Position(pos.getStartPosition(),
        pos.getEndPosition(state.compilationUnit.endPositions));
  }

  /**
   *
   * @param t an AST node
   * @param state
   * @return an error if the node matches the predicate, otherwise null
   */
  public abstract AstError produceError(T t, VisitorState state);

  public static class AstError {
    public Tree match;
    public String message;
    public SuggestedFix suggestedFix;

    public AstError(Tree match, String message, SuggestedFix suggestedFix) {
      this.message = message;
      this.suggestedFix = suggestedFix;
      this.match = match;
    }
  }
}
