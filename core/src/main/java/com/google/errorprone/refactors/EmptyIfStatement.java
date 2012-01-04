// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.refactors;

import static com.google.errorprone.matchers.Matchers.isNull;
import static com.google.errorprone.matchers.Matchers.nextStatement;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.sun.source.tree.Tree.Kind.IF;

import com.google.errorprone.ErrorProneCompiler;
import com.google.errorprone.Scanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;

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
public class EmptyIfStatement extends RefactoringMatcher<EmptyStatementTree> {

  /**
   * Match empty statement if:
   * - Parent statement is an if
   * - The then part of the parent if is an empty statement, and
   * - The else part of the parent if does not exist
   */
  @Override
  public boolean matches(EmptyStatementTree emptyStatementTree, VisitorState state) {
    boolean result = false;
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent.getKind() == IF) {
      IfTree parentAsIf = (IfTree)parent;
      result = (parentAsIf.getThenStatement() instanceof EmptyStatementTree) &&
          (parentAsIf.getElseStatement() == null);
    }
    return result;
  }

  /**
   * We suggest different fixes depending on what follows the parent if statement.
   * If there is no statement following the if, then suggest deleting the whole
   * if statement. If the next statement is a block, then suggest deleting the
   * empty then part of the if.  If the next statement is not a block, then also
   * suggest deleting the empty then part of the if.
   */
  @Override
  public Refactor refactor(EmptyStatementTree tree, VisitorState state) {
    boolean nextStmtIsNull = parentNode(nextStatement(isNull(StatementTree.class)))
        .matches(tree, state);

    assert(state.getPath().getParentPath().getLeaf().getKind() == IF);
    IfTree parent = (IfTree)state.getPath().getParentPath().getLeaf();
    SuggestedFix fix = new SuggestedFix();
    if (nextStmtIsNull) {
      // No following statements. Delete whole if.
      fix.delete(parent);
    } else {
      // There are more statements. Delete the empty then part of the if.
      fix.delete(parent.getThenStatement());
    }
    return new Refactor(parent, "empty statement after if", fix);
  }

  public static class Search extends Scanner {

    public Matcher<EmptyStatementTree> emptyIfMatcher = new EmptyIfStatement();
    @Override
    public Void visitEmptyStatement(EmptyStatementTree node, VisitorState state) {
      if (emptyIfMatcher.matches(node, state.withPath(getCurrentPath()))) {
        reportMatch(emptyIfMatcher, node, state);
      }
      return null;
    }

  }

  public static void main(String[] args) {
    System.exit(new ErrorProneCompiler.Builder()
        .search(new EmptyIfStatement.Search())
        .build()
        .compile(args));
  }
}
