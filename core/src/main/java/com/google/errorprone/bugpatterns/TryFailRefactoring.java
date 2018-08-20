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
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.Category.JUNIT;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.BugPattern.StandardTags.REFACTORING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.expressionStatement;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.sun.source.tree.Tree.Kind.UNION_TYPE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TryTree;
import com.sun.tools.javac.tree.JCTree;
import java.util.List;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "TryFailRefactoring",
    category = JUNIT,
    summary = "Prefer assertThrows to try/fail",
    severity = SUGGESTION,
    tags = REFACTORING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class TryFailRefactoring extends BugChecker implements TryTreeMatcher {

  private static final Matcher<StatementTree> FAIL_METHOD =
      expressionStatement(staticMethod().anyClass().named("fail"));

  @Override
  public Description matchTry(TryTree tree, VisitorState state) {
    List<? extends StatementTree> body = tree.getBlock().getStatements();
    if (body.isEmpty() || tree.getFinallyBlock() != null || tree.getCatches().size() != 1) {
      // TODO(cushon): support finally
      // TODO(cushon): support multiple catch blocks
      return NO_MATCH;
    }
    CatchTree catchTree = getOnlyElement(tree.getCatches());
    if (catchTree.getParameter().getType().getKind() == UNION_TYPE) {
      // TODO(cushon): handle multi-catch
      return NO_MATCH;
    }
    if (!FAIL_METHOD.matches(getLast(body), state)) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    StringBuilder fixPrefix = new StringBuilder();
    // try body statements, excluding the trailing `fail()`
    List<? extends StatementTree> throwingStatements =
        tree.getBlock().getStatements().subList(0, tree.getBlock().getStatements().size() - 1);
    if (throwingStatements.isEmpty()) {
      return NO_MATCH;
    }
    List<? extends StatementTree> catchStatements = catchTree.getBlock().getStatements();
    fix.addStaticImport("org.junit.Assert.assertThrows");
    if (!catchStatements.isEmpty()) {
      // TODO(cushon): pick a fresh name for the variable, if necessary
      fixPrefix.append(String.format("%s = ", state.getSourceForNode(catchTree.getParameter())));
    }
    fixPrefix.append(
        String.format(
            "assertThrows(%s.class, () -> ",
            state.getSourceForNode(catchTree.getParameter().getType())));
    boolean useExpressionLambda =
        throwingStatements.size() == 1
            && getOnlyElement(throwingStatements).getKind() == Kind.EXPRESSION_STATEMENT;
    if (!useExpressionLambda) {
      fixPrefix.append("{");
    }
    fix.replace(
        ((JCTree) tree).getStartPosition(),
        ((JCTree) throwingStatements.iterator().next()).getStartPosition(),
        fixPrefix.toString());
    if (useExpressionLambda) {
      fix.postfixWith(
          ((ExpressionStatementTree) throwingStatements.iterator().next()).getExpression(), ")");
    } else {
      fix.postfixWith(getLast(throwingStatements), "});");
    }
    if (catchStatements.isEmpty()) {
      fix.replace(
          state.getEndPosition(getLast(throwingStatements)), state.getEndPosition(tree), "");
    } else {
      fix.replace(
          /* startPos= */ state.getEndPosition(getLast(throwingStatements)),
          /* endPos= */ ((JCTree) catchStatements.get(0)).getStartPosition(),
          "\n");
      fix.replace(state.getEndPosition(getLast(catchStatements)), state.getEndPosition(tree), "");
    }
    return describeMatch(tree, fix.build());
  }
}
