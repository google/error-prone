// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.checkers;

import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.EmptyStatementTree;

/**
 * This checker finds and fixes empty statements, for example:
 * if (foo == 10);
 * ;
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class EmptyStatementChecker extends ErrorChecker<EmptyStatementTree> {

  @Override
  public Matcher<EmptyStatementTree> matcher() {
    return new Matcher<EmptyStatementTree>() {
      // All empty statements match, so just return true.
      @Override
      public boolean matches(EmptyStatementTree t, VisitorState state) {
        return true;
      }
    };
  }

  @Override
  public com.google.errorprone.checkers.ErrorChecker.AstError produceError(
      EmptyStatementTree emptyStatementTree, VisitorState state) {
    return new AstError(
        emptyStatementTree,
        "empty statement",
        SuggestedFix.delete(getPosition(emptyStatementTree)));
  }
  

}
