/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import java.util.List;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "AssertThrowsMultipleStatements",
    summary = "The lambda passed to assertThrows should contain exactly one statement",
    severity = SeverityLevel.WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class AssertThrowsMultipleStatements extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      staticMethod().onClass("org.junit.Assert").named("assertThrows");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    ExpressionTree arg = getLast(tree.getArguments());
    if (!(arg instanceof LambdaExpressionTree)) {
      return NO_MATCH;
    }
    Tree body = ((LambdaExpressionTree) arg).getBody();
    if (!(body instanceof BlockTree)) {
      return NO_MATCH;
    }
    List<? extends StatementTree> statements = ((BlockTree) body).getStatements();
    if (statements.size() <= 1) {
      return NO_MATCH;
    }
    StatementTree last = getLast(statements);
    int startPosition = ((JCTree) statements.get(0)).getStartPosition();
    int endPosition = state.getEndPosition(statements.get(statements.size() - 2));
    SuggestedFix.Builder fix = SuggestedFix.builder();
    // if the last statement is an expression, convert from a block to expression lambda
    if (last instanceof ExpressionStatementTree) {
      fix.replace(body, state.getSourceForNode(((ExpressionStatementTree) last).getExpression()));
    } else {
      fix.replace(startPosition, endPosition, "");
    }
    fix.prefixWith(
        state.findEnclosing(StatementTree.class),
        state.getSourceCode().subSequence(startPosition, endPosition).toString());
    return describeMatch(last, fix.build());
  }
}
