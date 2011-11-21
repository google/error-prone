// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.checkers;

import com.google.errorprone.ErrorCollectingTreeScanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.checkers.ErrorChecker.AstError;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.Tree.Kind;

import java.util.ArrayList;
import java.util.List;

/**
 * This checker finds and fixes empty statements after an if, for example:
 * if (foo == 10);
 *  
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class EmptyIfStatementChecker extends ErrorChecker<EmptyStatementTree> {

  @Override
  public Matcher<EmptyStatementTree> matcher() {
    return new Matcher<EmptyStatementTree>() {
      @Override
      public boolean matches(EmptyStatementTree t, VisitorState state) {
        return state.getPath().getParentPath().getLeaf().getKind() == Kind.IF;
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
  
  public static class Scanner extends ErrorCollectingTreeScanner {
    public ErrorChecker<EmptyStatementTree> emptyIfChecker = 
        new EmptyIfStatementChecker();

    @Override 
    public List<AstError> visitEmptyStatement(EmptyStatementTree tree, VisitorState visitorState) {
      List<AstError> result = new ArrayList<AstError>();
      AstError error = emptyIfChecker
          .check(tree, visitorState.withPath(getCurrentPath()));
      if (error != null) {
        result.add(error);
      }

      super.visitEmptyStatement(tree, visitorState);
      return result;
    }
  }

  

}
