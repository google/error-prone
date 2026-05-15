/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ErrorProneComment;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import java.util.List;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "assertThrows calls with lambdas containing a single statement can be expressed more"
            + " concisely",
    severity = WARNING)
public class AssertThrowsBlockToExpression extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      staticMethod().onClass("org.junit.Assert").named("assertThrows");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    if (!(tree.getArguments().getLast() instanceof LambdaExpressionTree lambdaExpressionTree
        && lambdaExpressionTree.getBody() instanceof BlockTree blockTree)) {
      return NO_MATCH;
    }
    List<? extends StatementTree> statements = blockTree.getStatements();
    if (statements.size() != 1) {
      return NO_MATCH;
    }
    if (!(statements.getFirst() instanceof ExpressionStatementTree expressionStatementTree)) {
      return NO_MATCH;
    }
    ExpressionTree expression = expressionStatementTree.getExpression();
    SuggestedFix.Builder fix = SuggestedFix.builder();
    replaceAndKeepComments(fix, getStartPosition(blockTree), getStartPosition(expression), state);
    replaceAndKeepComments(
        fix, state.getEndPosition(expression), state.getEndPosition(blockTree), state);
    return describeMatch(blockTree, fix.build());
  }

  private static void replaceAndKeepComments(
      SuggestedFix.Builder fix, int start, int end, VisitorState state) {
    ImmutableList<String> comments =
        state.getOffsetTokens(start, end).stream()
            .flatMap(errorProneToken -> errorProneToken.comments().stream())
            .filter(comment -> !comment.getText().isEmpty())
            .map(ErrorProneComment::getText)
            .collect(toImmutableList());
    String replacement =
        comments.isEmpty() ? "" : comments.stream().collect(joining("\n", "\n", "\n"));
    fix.replace(start, end, replacement);
  }
}
