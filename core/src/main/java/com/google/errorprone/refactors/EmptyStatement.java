// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.refactors;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.ON_BY_DEFAULT;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
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
@BugPattern(
    name = "Empty statement",
    category = JDK,
    severity = WARNING,
    maturity = ON_BY_DEFAULT,
    summary = "Empty statement",
    explanation =
        "An empty statement has no effect on the program. Consider removing it.")
public class EmptyStatement extends RefactoringMatcher<EmptyStatementTree> {

  @Override
  public boolean matches(EmptyStatementTree emptyStatementTree, VisitorState state) {
    return true;
  }

  @Override
  public Refactor refactor(
      EmptyStatementTree emptyStatementTree, VisitorState state) {
    return new Refactor(
        emptyStatementTree,
        "empty statement",
        new SuggestedFix().delete(emptyStatementTree));
  }

}
