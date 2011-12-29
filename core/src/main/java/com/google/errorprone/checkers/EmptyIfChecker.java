// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.checkers;

import com.google.errorprone.ErrorCollectingTreeScanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;

import java.util.ArrayList;
import java.util.List;

import static com.sun.source.tree.Tree.Kind.BLOCK;
import static com.sun.source.tree.Tree.Kind.IF;
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
public class EmptyIfChecker extends ErrorChecker<EmptyStatementTree> {

  /**
   * Match empty statement if:
   * - Parent statement is an if
   * - The then part of the parent if is an empty statement, and
   * - The else part of the parent if does not exist
   */
  @Override
  public Matcher<EmptyStatementTree> matcher() {
    return new Matcher<EmptyStatementTree>() {
      @Override
      public boolean matches(EmptyStatementTree node, VisitorState state) {
        boolean result = false;
        Tree parent = state.getPath().getParentPath().getLeaf();
        if (parent.getKind() == IF) {
          IfTree parentAsIf = (IfTree)parent;
          result = (parentAsIf.getThenStatement() instanceof EmptyStatementTree) && 
              (parentAsIf.getElseStatement() == null);
        }
        return result;
      }
    };
  }

  /**
   * We suggest different fixes depending on what follows the parent if statement. 
   * If there is no statement following the if, then suggest deleting the whole 
   * if statement. If the next statement is a block, then suggest deleting the 
   * empty then part of the if.  If the next statement is not a block, then also 
   * suggest deleting the empty then part of the if.
   */
  @Override
  public com.google.errorprone.checkers.ErrorChecker.AstError produceError(
      EmptyStatementTree tree, VisitorState state) {
    
    boolean nextStmtIsNull = parentNode(nextStatement(isNull(StatementTree.class)))
        .matches(tree, state);
    
    boolean nextStmtIsBlock = false;
    if (!nextStmtIsNull) {
      nextStmtIsBlock = parentNode(nextStatement(kindIs(BLOCK, StatementTree.class)))
          .matches(tree, state);
    }
    
    assert(state.getPath().getParentPath().getLeaf().getKind() == IF);
    IfTree parent = (IfTree)state.getPath().getParentPath().getLeaf();
    if (nextStmtIsNull) {
      // No following statements. Delete whole if.
      return new AstError(
          parent,
          "empty statement after if",
          SuggestedFix.delete(getPosition(parent)));
    } else if (nextStmtIsBlock) {
      // Following statement is a block. Delete the empty then part of the if.
      return new AstError(
          parent,
          "empty statement after if",
          SuggestedFix.delete(getPosition(parent.getThenStatement())));
    } else {
      // Following statement is not a block. Delete the empty then part of the if.
      return new AstError(
          parent,
          "empty statement after if",
          SuggestedFix.delete(getPosition(parent.getThenStatement())));
    }
  }
  
  public static class Scanner extends ErrorCollectingTreeScanner {
    public ErrorChecker<EmptyStatementTree> emptyIfChecker = 
        new EmptyIfChecker();
    
    @Override 
    public List<AstError> visitEmptyStatement(EmptyStatementTree node, 
        VisitorState visitorState) {
      List<AstError> result = new ArrayList<AstError>();
      AstError error = emptyIfChecker
          .check(node, visitorState.withPath(getCurrentPath()));
      if (error != null) {
        result.add(error);
      }
      
      super.visitEmptyStatement(node, visitorState);
      return result;
    }
  }
}
