/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.errorprone;

import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ExpressionStatementTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.Tree;

/**
 * A {@link BugChecker} that flags {@link ExpressionStatementTree}s that satisfy both of the
 * following:
 *
 * <ul>
 *   <li>The text of the tree is the same as the given {@code expressionStatement}, and
 *   <li>The given {@code matcher} matches the tree
 * </ul>
 *
 * <p>Useful for testing {@link Matcher}s.
 */
public abstract class MatcherChecker extends BugChecker implements ExpressionStatementTreeMatcher {
  private final String expressionStatement;
  private final Matcher<Tree> matcher;

  public MatcherChecker(String expressionStatement, Matcher<Tree> matcher) {
    this.expressionStatement = expressionStatement;
    this.matcher = matcher;
  }

  @Override
  public Description matchExpressionStatement(ExpressionStatementTree tree, VisitorState state) {
    return (tree.toString().equals(expressionStatement) && matcher.matches(tree, state))
        ? describeMatch(tree)
        : Description.NO_MATCH;
  }
}
