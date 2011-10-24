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

package com.google.errorprone.matchers;

import com.google.errorprone.SuggestedFix;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.errorprone.matchers.Matchers.*;
import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;
import static com.sun.source.tree.Tree.Kind.IF;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class DeadExceptionMatcher extends ErrorProducingMatcher<NewClassTree> {
  @Override
  public AstError matchWithError(NewClassTree newClassTree, VisitorState state) {
    TreeHolder<StatementTree> enclosingStatement = TreeHolder.create();
    AtomicBoolean isLastStatementInBlock = new AtomicBoolean();

    if (allOf(
        // The "new X()" expression is the entire statement (and save that statement)
        parentNode(capture(enclosingStatement, kindIs(EXPRESSION_STATEMENT))),
        // X is an Exception
        isSubtypeOf(state.symtab.exceptionType),
        // Save whether the new Exception statement is the last in the block
        storeToBoolean(isLastStatementInBlock, anyOf(
            enclosingBlock(lastStatement(same(enclosingStatement))),
            // it could also be a bare if statement with no braces
            parentNode(parentNode(kindIs(IF)))))
    ).matches(newClassTree, state)) {
      DiagnosticPosition pos = ((JCTree) newClassTree).pos();
      DiagnosticPosition statementPos = ((JCTree)enclosingStatement.get()).pos();

      SuggestedFix suggestedFix = isLastStatementInBlock.get()
          ? new SuggestedFix(pos.getStartPosition(), pos.getStartPosition(), "throw ")
          : new SuggestedFix(statementPos.getStartPosition(),
          statementPos.getEndPosition(state.compilationUnit.endPositions), "");
      return new AstError(newClassTree, "Exception created but not thrown, and reference is lost",
          suggestedFix);
    }
    return null;
  }
}
