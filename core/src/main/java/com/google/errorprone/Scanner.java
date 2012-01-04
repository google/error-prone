// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.errorprone;

import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.refactors.RefactoringMatcher;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePathScanner;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class Scanner extends TreePathScanner<Void, VisitorState> {
  protected <T extends Tree> void reportMatch(Matcher<T> matcher, T match, VisitorState state) {
    state.getMatchListener().onMatch(match);
    if (matcher instanceof RefactoringMatcher) {
      RefactoringMatcher<T> refactoringMatcher = (RefactoringMatcher<T>) matcher;
      state.getRefactorListener().onRefactor(refactoringMatcher.refactor(match, state));
    }
  }
}
