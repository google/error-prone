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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.*;
import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;
import static com.sun.source.tree.Tree.Kind.IF;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "DeadException",
    summary = "Exception created but not thrown",
    explanation =
        "The exception is created with new, but is not thrown, and the reference is lost.",
    category = JDK, severity = WARNING, maturity = EXPERIMENTAL)
public class DeadException extends DescribingMatcher<NewClassTree> {

  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(NewClassTree newClassTree, VisitorState state) {
    return allOf(
        parentNode(kindIs(EXPRESSION_STATEMENT)),
        isSubtypeOf(state.getSymtab().exceptionType)
    ).matches(newClassTree, state);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Description describe(NewClassTree newClassTree, VisitorState state) {
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
    return new Description(newClassTree, diagnosticMessage, suggestedFix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private DeadException matcher = new DeadException();

    @Override
    public Void visitNewClass(NewClassTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitNewClass(node, visitorState);
    }
  }

}
