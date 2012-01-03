// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.refactors;

import com.google.errorprone.RefactoringVisitorState;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.EmptyStatementTree;

/**
 * This checker finds and fixes empty statements, for example:
 * if (foo == 10);
 * ;
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class EmptyStatement extends RefactoringMatcher<EmptyStatementTree> {

  @Override
  public boolean matches(EmptyStatementTree emptyStatementTree, VisitorState state) {
    return true;
  }

  @Override
  public Refactor refactor(
      EmptyStatementTree emptyStatementTree, RefactoringVisitorState state) {
    return new Refactor(
        emptyStatementTree,
        "empty statement",
        new SuggestedFix().delete(emptyStatementTree));
  }

}
