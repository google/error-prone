// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.checkers;

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
public class EmptyStatementChecker extends DescribingMatcher<EmptyStatementTree> {

  @Override
  public boolean matches(EmptyStatementTree emptyStatementTree, VisitorState state) {
    return true;
  }

  @Override
  public MatchDescription describe(
      EmptyStatementTree emptyStatementTree, VisitorState state) {
    return new MatchDescription(
        emptyStatementTree,
        "empty statement",
        new SuggestedFix().delete(emptyStatementTree));
  }

}
