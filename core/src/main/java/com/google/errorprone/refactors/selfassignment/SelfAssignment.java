// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.errorprone.refactors.selfassignment;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.isSelfAssignment;

import com.google.errorprone.BugPattern;
import com.google.errorprone.Scanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.refactors.RefactoringMatcher;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.Tree;

/**
 * TODO(eaftan): doesn't seem to be visiting assignments in declarations:
 * int i = 10;  // this can't be an error, but why aren't we visiting it?
 * or incrementing assignments:
 * i += 10; // maybe this becomes i = i + 10 by this compiler phase?
 * 
 * 
 * Also consider cases where the parent is not a statement or there is
 * no parent?
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 *
 */
@BugPattern(
    name = "Self assignment",
    category = JDK,
    severity = ERROR,
    maturity = EXPERIMENTAL,
    summary = "Variable assigned to itself",
    explanation = "The left-hand side and right-hand side of this assignment are the same. " +
    		"It has no effect.")
public class SelfAssignment extends RefactoringMatcher<AssignmentTree> {

  @Override
  public boolean matches(AssignmentTree t, VisitorState state) {
    return isSelfAssignment()
        .matches(t, state);
  }

  @Override
  public Refactor refactor(AssignmentTree t,
      VisitorState state) {
    // delete statement that is parent of self assignment
    Tree parent = state.getPath().getParentPath().getLeaf();
    return new Refactor(t, refactorMessage, new SuggestedFix().delete(parent));
  }

  public static class Search extends Scanner {
    public Matcher<AssignmentTree> selfAssignmentMatcher = new SelfAssignment();
    @Override
    public Void visitAssignment(AssignmentTree node, VisitorState state) {
      if (selfAssignmentMatcher.matches(node, state.withPath(getCurrentPath()))) {
        reportMatch(selfAssignmentMatcher, node, state);
      }
      return null;
    }
  }

}
