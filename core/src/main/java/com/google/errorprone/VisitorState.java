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

package com.google.errorprone;

import com.google.errorprone.refactors.RefactoringMatcher.Refactor;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class VisitorState {

  private final RefactorListener refactorListener;
  private final MatchListener matchListener;
  private final Context context;
  private final TreePath path;

  private VisitorState(Context context, TreePath path,
      RefactorListener refactorListener, MatchListener matchListener) {
    this.context = context;
    this.path = path;
    this.refactorListener = refactorListener;
    this.matchListener = matchListener;
  }

  public VisitorState(Context context, RefactorListener listener) {
    this(context, null, listener, new MatchListener() {
      @Override
      public void onMatch(Tree tree) {
      }
    });
  }

  public VisitorState(Context context, MatchListener listener) {
    this(context, null, new RefactorListener() {
      @Override
      public void onRefactor(Refactor refactor) {}
    }, listener);
  }

  public VisitorState withPath(TreePath path) {
    return new VisitorState(context, path, refactorListener, matchListener);
  }

  public TreePath getPath() {
    return path;
  }

  public TreeMaker getTreeMaker() {
    return TreeMaker.instance(context);
  }

  public Types getTypes() {
    return Types.instance(context);
  }

  public Symtab getSymtab() {
    return Symtab.instance(context);
  }

  public RefactorListener getRefactorListener() {
    return refactorListener;
  }

  public MatchListener getMatchListener() {
    return matchListener;
  }
}
