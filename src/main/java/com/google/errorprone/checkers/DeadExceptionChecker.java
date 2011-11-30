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

package com.google.errorprone.checkers;

import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;

import static com.google.errorprone.fixes.SuggestedFix.delete;
import static com.google.errorprone.fixes.SuggestedFix.prefixWith;
import static com.google.errorprone.matchers.Matchers.*;
import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;
import static com.sun.source.tree.Tree.Kind.IF;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class DeadExceptionChecker extends ErrorChecker<NewClassTree> {

  @Override
  @SuppressWarnings("unchecked")
  public Matcher<NewClassTree> matcher() {
    return allOf(
        parentNode(kindIs(EXPRESSION_STATEMENT)),
        isSubtypeOf(getSymbolTable().exceptionType));
  }

  @Override
  public AstError produceError(NewClassTree newClassTree, VisitorState state) {
    StatementTree parent = (StatementTree) getPath().getParentPath().getLeaf();

    boolean isLastStatement = anyOf(
        enclosingBlock(lastStatement(same(parent))),
        // it could also be a bare if statement with no braces
        parentNode(parentNode(kindIs(IF))))
        .matches(newClassTree, state);

    SuggestedFix suggestedFix = isLastStatement
        ? prefixWith(getPosition(newClassTree), "throw ")
        : delete(getPosition(parent));
    return new AstError(newClassTree, "Exception created but not thrown, and reference is lost",
        suggestedFix);
  }
}
