// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.checkers;

import com.google.errorprone.ErrorCollectingTreeScanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;

import java.util.ArrayList;
import java.util.List;

import static com.sun.source.tree.Tree.Kind.BLOCK;
import static com.google.errorprone.matchers.Matchers.*;

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

  /**
   * Match if:
   * - The then part is an empty statement, and
   * - The else part does not exist
   */
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

  /**
   * We suggest different fixes depending on what follows the if statement.
   * If there is no statement following the if, then suggest deleting the 
   * whole if statement. If the next statement is a block, then suggest 
   * deleting the empty then part of the if.  If the next statement is not a 
   * block, then also suggest deleting the empty then part of the if.
   * 
   * TODO(eaftan): In the case where there is a non-block statement following 
   * the if, it's actually unclear what the right fix is. Typically the 
   * correct fix is going to be to create a block containing 1 or more of the
   * following statements, but it requires inspection of the code to determine
   * how many statements should be in that block (i.e, should be executed
   * conditionally).  On the other hand, the semantics-preserving fix would
   * be to just delete the whole if statement.
   */
  @Override
  public com.google.errorprone.checkers.ErrorChecker.AstError produceError(
      IfTree tree, VisitorState state) {
    
    boolean nextStmtIsNull = nextStatement(isNull(StatementTree.class))
        .matches(tree, state);
    
    boolean nextStmtIsBlock = false;
    if (!nextStmtIsNull) {
      nextStmtIsBlock = nextStatement(kindIs(BLOCK, StatementTree.class))
          .matches(tree, state);
    }
    
    if (nextStmtIsNull) {
      // No following statements. Delete whole if.
      return new AstError(
          tree,
          "empty statement after if",
          SuggestedFix.delete(getPosition(tree)));
    } else if (nextStmtIsBlock) {
      // Following statement is a block. Delete the empty then part of the if.
      StatementTree empty = tree.getThenStatement();
      return new AstError(
          empty,
          "empty statement after if",
          SuggestedFix.delete(getPosition(empty)));
    } else {
      // Following statement is not a block. Delete the empty then part of the if.
      StatementTree empty = tree.getThenStatement();
      return new AstError(
          empty,
          "empty statement after if",
          SuggestedFix.delete(getPosition(empty)));
    }
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
