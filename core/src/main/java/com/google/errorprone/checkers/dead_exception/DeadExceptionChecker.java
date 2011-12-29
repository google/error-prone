/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.checkers.dead_exception;

import static com.google.errorprone.BugPattern.Category.UNIVERSAL;
import static com.google.errorprone.BugPattern.MaturityLevel.ON_BY_DEFAULT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingBlock;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.lastStatement;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.google.errorprone.matchers.Matchers.same;
import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;
import static com.sun.source.tree.Tree.Kind.IF;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.checkers.DescribingMatcher;
import com.google.errorprone.fixes.SuggestedFix;

import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(
    name = "Dead exception",
    category = UNIVERSAL,
    severity = ERROR,
    maturity = ON_BY_DEFAULT,
    summary = "Exception created but not thrown",
    explanation =
        "The exception is created with new, but is not thrown, and the reference is lost.")
public class DeadExceptionChecker extends DescribingMatcher<NewClassTree> {

  @Override
  public boolean matches(NewClassTree newClassTree, VisitorState state) {
    return allOf(
        parentNode(kindIs(EXPRESSION_STATEMENT)),
        isSubtypeOf(state.getSymtab().exceptionType))
        .matches(newClassTree, state);
  }

  @Override
  public MatchDescription describe(NewClassTree newClassTree, VisitorState state) {
    StatementTree parent = (StatementTree) state.getPath().getParentPath().getLeaf();

    boolean isLastStatement = anyOf(
        enclosingBlock(lastStatement(same(parent))),
        // it could also be a bare if statement with no braces
        parentNode(parentNode(kindIs(IF))))
        .matches(newClassTree, state);

    SuggestedFix suggestedFix = new SuggestedFix();
    if (isLastStatement) {
      suggestedFix.prefixWith(newClassTree, "throw ");
    } else {
      suggestedFix.delete(parent);
    }
    return new MatchDescription(newClassTree,
        "Exception created but not thrown, and reference is lost",
        suggestedFix);
  }


}
