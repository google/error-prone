// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.errorprone;

import com.google.errorprone.refactors.RefactoringMatcher.Refactor;

import com.sun.source.tree.Tree;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class RefactoringScanner extends SearchingScanner {

  @Override
  protected void reportMatch(Tree match, VisitorState state) {
    super.reportMatch(match, state);
  }

  protected void refactor(Refactor refactor, VisitorState state) {
    state.getRefactorListener().onRefactor(refactor);
  }

}
