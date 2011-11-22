// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.checkers;

import com.google.errorprone.ErrorCollectingTreeScanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.checkers.ErrorChecker.AstError;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.Tree.Kind;

import java.util.ArrayList;
import java.util.List;

/**
 * This checker finds and fixes empty statements after an if, with no else 
 * part. For example:
 * if (foo == 10);
 * 
 * It attempts to match javac's -Xlint:empty warning behavior, which can
 * be found in com/sun/tools/javac/comp/Check.java.
 *  
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class EmptyIfChecker extends ErrorChecker<IfTree> {

  @Override
  public Matcher<IfTree> matcher() {
    return new Matcher<IfTree>() {
      @Override
      public boolean matches(IfTree tree, VisitorState state) {
        return tree.getThenStatement() instanceof EmptyStatementTree && 
               tree.getElseStatement() == null;
      }
    };
  }

  @Override
  public com.google.errorprone.checkers.ErrorChecker.AstError produceError(
      IfTree tree, VisitorState state) {
    EmptyStatementTree empty = (EmptyStatementTree)tree.getThenStatement();
    return new AstError(
        empty,
        "empty statement",
        SuggestedFix.delete(getPosition(empty)));
  }
  
  public static class Scanner extends ErrorCollectingTreeScanner {
    public ErrorChecker<IfTree> emptyIfChecker = 
        new EmptyIfChecker();

    @Override 
    public List<AstError> visitIf(IfTree tree, VisitorState visitorState) {
      List<AstError> result = new ArrayList<AstError>();
      AstError error = emptyIfChecker
          .check(tree, visitorState.withPath(getCurrentPath()));
      if (error != null) {
        result.add(error);
      }

      super.visitIf(tree, visitorState);
      return result;
    }
  }

  

}
