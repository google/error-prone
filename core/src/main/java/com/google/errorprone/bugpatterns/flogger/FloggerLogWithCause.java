/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.flogger;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFix.postfixWith;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.lang.Boolean.TRUE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CatchTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.List;

/**
 * Flags cases where there is an exception available that could be set as the cause in a log
 * message.
 */
@BugPattern(
    name = "FloggerLogWithCause",
    summary =
        "Setting the caught exception as the cause of the log message may provide more context for"
            + " anyone debugging errors.",
    severity = WARNING)
public final class FloggerLogWithCause extends BugChecker implements CatchTreeMatcher {

  private static final Matcher<ExpressionTree> LOG_MATCHER =
      instanceMethod().onDescendantOf("com.google.common.flogger.LoggingApi").named("log");

  private static final Matcher<ExpressionTree> WITH_CAUSE =
      instanceMethod().onDescendantOf("com.google.common.flogger.LoggingApi").named("withCause");

  private static final Matcher<ExpressionTree> HIGH_LEVEL =
      instanceMethod()
          .onDescendantOf("com.google.common.flogger.AbstractLogger")
          .namedAnyOf("atWarning", "atSevere");

  @Override
  public Description matchCatch(CatchTree tree, VisitorState state) {
    if (isSuppressed(tree.getParameter())) {
      return Description.NO_MATCH;
    }
    List<? extends StatementTree> statements = tree.getBlock().getStatements();
    if (statements.size() != 1) {
      return NO_MATCH;
    }
    StatementTree statementTree = statements.get(0);
    if (!(statementTree instanceof ExpressionStatementTree)) {
      return NO_MATCH;
    }
    ExpressionTree expressionTree = ((ExpressionStatementTree) statementTree).getExpression();
    if (!LOG_MATCHER.matches(expressionTree, state)) {
      return NO_MATCH;
    }
    boolean isHighLevelLog = false;
    for (ExpressionTree receiver = expressionTree;
        receiver instanceof MethodInvocationTree;
        receiver = getReceiver(receiver)) {
      if (WITH_CAUSE.matches(receiver, state)) {
        return NO_MATCH;
      }
      if (HIGH_LEVEL.matches(receiver, state)) {
        isHighLevelLog = true;
      }
    }
    if (!isHighLevelLog) {
      return NO_MATCH;
    }
    Symbol parameter = getSymbol(tree.getParameter());
    boolean exceptionUsed =
        new TreeScanner<Boolean, Void>() {
          @Override
          public Boolean visitIdentifier(IdentifierTree node, Void unused) {
            return parameter.equals(getSymbol(node));
          }

          @Override
          public Boolean reduce(Boolean a, Boolean b) {
            return TRUE.equals(a) || TRUE.equals(b);
          }
        }.scan(tree.getBlock(), null);
    if (exceptionUsed) {
      return NO_MATCH;
    }
    String withCause = String.format(".withCause(%s)", tree.getParameter().getName());
    return describeMatch(expressionTree, postfixWith(getReceiver(expressionTree), withCause));
  }
}
