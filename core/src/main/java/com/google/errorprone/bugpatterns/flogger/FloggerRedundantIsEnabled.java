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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.sun.source.tree.Tree.Kind.BLOCK;
import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.List;
import java.util.Optional;

/** @author mariasam@google.com (Maria Sam) */
@BugPattern(
    name = "FloggerRedundantIsEnabled",
    summary =
        "Logger level check is already implied in the log() call. "
            + "An explicit at[Level]().isEnabled() check is redundant.",
    severity = WARNING)
public class FloggerRedundantIsEnabled extends BugChecker implements IfTreeMatcher {

  private static final String FLOGGER = "com.google.common.flogger.FluentLogger";
  private static final String FLOGGER_API = "com.google.common.flogger.FluentLogger.Api";

  private static final Matcher<ExpressionTree> AT_LEVEL =
      instanceMethod()
          .onDescendantOf(FLOGGER)
          .namedAnyOf(
              "atInfo", "atConfig", "atFine", "atFiner", "atFinest", "atWarning", "atSevere")
          .withParameters();
  private static final Matcher<ExpressionTree> IS_ENABLED =
      instanceMethod().onDescendantOf(FLOGGER_API).named("isEnabled").withParameters();
  private static final Matcher<ExpressionTree> LOG =
      instanceMethod().onDescendantOf(FLOGGER_API).named("log");

  @Override
  public Description matchIf(IfTree ifTree, VisitorState state) {
    // No else-statement.
    if (ifTree.getElseStatement() != null) {
      return NO_MATCH;
    }

    // If then-block contains exactly one expression and it is a `log` invocation, extract it.
    Optional<MethodInvocationTree> methodCall = extractLoneLogInvocation(ifTree, state);
    if (!methodCall.isPresent()) {
      return NO_MATCH;
    }
    MethodInvocationTree logInvocation = methodCall.get();
    ExpressionTree ifCondition = ifTree.getCondition();
    // The if-condition is just a call to `.isEnabled()` (possibly negated).
    ExpressionTree unwrappedIfCondition = unwrap(ifCondition);
    if (IS_ENABLED.matches(unwrappedIfCondition, state)
        && sameLoggerAtSameLevel(unwrappedIfCondition, logInvocation, state)) {
      // Replace entire if-tree with just the `.log()` call.
      return describeMatch(
          ifTree, SuggestedFix.replace(ifTree, state.getSourceForNode(logInvocation) + ";"));
    }

    // Remove `.isEnabled()` call from complex if-condition.
    return fixBinaryIfCondition(ifCondition, logInvocation, state)
        .map(fix -> describeMatch(ifTree, fix))
        .orElse(NO_MATCH);
  }

  /**
   * Examines the then-block of the given if-tree. If the then-block contains exactly one statement,
   * and that one statement is a Flogger `log` method invocation, then returns that statement as a
   * {@link MethodInvocationTree}. Else returns an empty Optional.
   *
   * <p>Adapted from {@link com.google.errorprone.bugpatterns.ImplementAssertionWithChaining}.
   */
  private static Optional<MethodInvocationTree> extractLoneLogInvocation(
      IfTree ifTree, VisitorState state) {
    // Get lone statement from then-block.
    StatementTree thenStatement = ifTree.getThenStatement();
    while (thenStatement.getKind() == BLOCK) {
      List<? extends StatementTree> statements = ((BlockTree) thenStatement).getStatements();
      if (statements.size() != 1) {
        return Optional.empty();
      }
      thenStatement = getOnlyElement(statements);
    }

    // Check if that lone statement is a Flogger `log` method invocation, and cast.
    if (thenStatement.getKind() == EXPRESSION_STATEMENT) {
      ExpressionTree thenExpression = ((ExpressionStatementTree) thenStatement).getExpression();
      if (LOG.matches(thenExpression, state)) {
        return Optional.of((MethodInvocationTree) thenExpression);
      }
    }
    return Optional.empty();
  }

  /** Strip any parentheses or `!` from the given expression. */
  private static ExpressionTree unwrap(ExpressionTree expr) {
    return expr.accept(
        new SimpleTreeVisitor<ExpressionTree, Void>() {

          @Override
          protected ExpressionTree defaultAction(Tree tree, Void unused) {
            return tree instanceof ExpressionTree ? (ExpressionTree) tree : null;
          }

          @Override
          public ExpressionTree visitParenthesized(
              ParenthesizedTree parenthesizedTree, Void unused) {
            return parenthesizedTree.getExpression().accept(this, null);
          }

          @Override
          public ExpressionTree visitUnary(UnaryTree unaryTree, Void unused) {
            return unaryTree.getExpression().accept(this, null);
          }
        },
        null);
  }

  /**
   * Determines whether the two given logger invocations are operations on the same logger, at the
   * same level.
   */
  private static boolean sameLoggerAtSameLevel(
      ExpressionTree expr1, ExpressionTree expr2, VisitorState state) {
    ExpressionTree atLevel1 = getReceiver(expr1);
    ExpressionTree atLevel2 = getReceiver(expr2);
    if (!AT_LEVEL.matches(atLevel1, state) || !AT_LEVEL.matches(atLevel2, state)) {
      return false;
    }
    Symbol atLevelSym1 = getSymbol(atLevel1);
    Symbol atLevelSym2 = getSymbol(atLevel2);
    if (atLevelSym1 == null || !atLevelSym1.equals(atLevelSym2)) {
      return false;
    }
    Symbol logger1 = getSymbol(getReceiver(atLevel1));
    Symbol logger2 = getSymbol(getReceiver(atLevel2));
    return logger1 != null && logger1.equals(logger2);
  }

  private static Optional<SuggestedFix> fixBinaryIfCondition(
      ExpressionTree ifCondition, MethodInvocationTree logInvocation, VisitorState state) {
    LoggerIsEnabledBinaryIfConditionScanner scanner =
        new LoggerIsEnabledBinaryIfConditionScanner(logInvocation, state);
    scanner.scan(ifCondition, null);

    return scanner.fix;
  }

  private static class LoggerIsEnabledBinaryIfConditionScanner extends TreeScanner<Void, Void> {

    private final ExpressionTree logInvocation;
    private final VisitorState state;
    public Optional<SuggestedFix> fix;

    public LoggerIsEnabledBinaryIfConditionScanner(
        ExpressionTree logInvocation, VisitorState state) {
      this.logInvocation = logInvocation;
      this.state = state;
      this.fix = Optional.empty();
    }

    @Override
    public Void visitBinary(BinaryTree binaryTree, Void unused) {
      ExpressionTree left = unwrap(binaryTree.getLeftOperand());
      ExpressionTree right = unwrap(binaryTree.getRightOperand());
      if (IS_ENABLED.matches(left, state) && sameLoggerAtSameLevel(logInvocation, left, state)) {
        // `isEnabled` on left. Replace binary condition with just the right side.
        this.fix =
            Optional.of(
                SuggestedFix.replace(
                    binaryTree, state.getSourceForNode(binaryTree.getRightOperand())));
        return null;
      }
      if (IS_ENABLED.matches(right, state) && sameLoggerAtSameLevel(logInvocation, right, state)) {
        // `isEnabled` on right. Replace binary condition with just the left side.
        this.fix =
            Optional.of(
                SuggestedFix.replace(
                    binaryTree, state.getSourceForNode(binaryTree.getLeftOperand())));
        return null;
      }
      return super.visitBinary(binaryTree, null);
    }
  }
}
