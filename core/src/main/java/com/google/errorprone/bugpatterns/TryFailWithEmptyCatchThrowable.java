/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JUNIT;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;

import java.util.List;

/**
 * A bug checker for the following code pattern:
 *
 * <pre>
 * try {
 *   // do something
 *   Assert.fail();
 * } catch (Throwable t) {
 *   // empty or only comments
 * }
 * </pre>
 *
 * <p>Matches the following {@code fail} methods:
 * <ul>
 * <li>{@link org.junit.Assert#fail()}
 * <li>{@link org.junit.Assert#fail(String)}
 * <li>{@link junit.framework.Assert#fail()}
 * <li>{@link junit.framework.Assert#fail(String)}
 * </ul>
 *
 * <p>Possible improvements/generalizations of this matcher:
 * <ul>
 * <li>catch (Error)
 * <li>fail doesn't need to be the last statement; not only fail(), but any
 *     occurrence of assert* method in the try block
 * <li>support multiple catch() blocks
 * </ul>
 *
 * @author adamwos@google.com (Adam Wos)
 */
@BugPattern(name = "TryFailWithEmptyCatchThrowable",
    summary = "This test will never fail. Don't catch (Throwable).",
    explanation = "A common pattern for testing for expected exceptions is to execute code in a "
        + "try block, with a fail() at the end, catching an expected exception. However, if the "
        + "catch clause catches Throwable, and doesn't do any verification (e.g. instanceof) of "
        + "the caught object, such a test always passes.",
    category = JUNIT, maturity = EXPERIMENTAL, severity = ERROR)
public class TryFailWithEmptyCatchThrowable extends BugChecker implements TryTreeMatcher {

  @SuppressWarnings("unchecked")
  private static final Matcher<MethodInvocationTree> ASSERT_FAIL_METHOD = anyOf(
      methodSelect(staticMethod("org.junit.Assert", "fail")),
      methodSelect(staticMethod("junit.framework.Assert", "fail")));

  private static final Matcher<VariableTree> THROWABLE_TYPE =
      isSameType("java.lang.Throwable");

  private static final Matcher<Tree> EMPTY_STATEMENT = Matchers.kindIs(Kind.EMPTY_STATEMENT);

  @Override
  public Description matchTry(TryTree tree, VisitorState state) {
    System.err.println("Matching try tree " + tree);
    return describeMatch(tree, new SuggestedFix().delete(tree.getCatches().get(0)));
//    if (tryTreeMatches(tree, state)) {
//      VariableTree catchParameter = tree.getCatches().get(0).getParameter();
//      SuggestedFix fix = new SuggestedFix().replace(
//          catchParameter, "Exception " + catchParameter.getName());
//      return describeMatch(tree, fix);
//    } else {
//      return Description.NO_MATCH;
//    }
  }

  private boolean tryTreeMatches(TryTree tryTree, VisitorState state) {
    return true;
//
//    System.out.println("foo!");
//    // Get the last statement of a non-empty try block.
//    BlockTree tryBlock = tryTree.getBlock();
//    List<? extends StatementTree> statements = tryBlock.getStatements();
//    if (statements.isEmpty()) {
//      return false;
//    }
//    // TODO(adamwos): Doesn't need to be last statement; also support fail(),
//    // assert*() in the method body.
//    StatementTree lastStatement = statements.get(statements.size() - 1);
//    if (!(lastStatement instanceof ExpressionStatementTree)) {
//      return false;
//    }
//
//    // Check if the last statement is a fail() method invocation.
//    ExpressionStatementTree lastExpressionStatementTree = (ExpressionStatementTree) lastStatement;
//    ExpressionTree lastExpression = lastExpressionStatementTree.getExpression();
//    if (!(lastExpression instanceof MethodInvocationTree)) {
//      return false;
//    }
//    if (!ASSERT_FAIL_METHOD.matches((MethodInvocationTree) lastExpression, state)) {
//      return false;
//    }
//
//    // Verify that the only catch clause catches Throwable
//    List<? extends CatchTree> catches = tryTree.getCatches();
//    if (catches.size() != 1) {
//      // TODO(adamwos): this could be supported - only the last catch would need
//      // to be checked, as there's nothing else that can be more general than
//      // catch (Throwable).
//      return false;
//    }
//    CatchTree catchTree = catches.get(0);
//    VariableTree catchType = catchTree.getParameter();
//    // TODO(adamwos): Error could be supported
//    if (!THROWABLE_TYPE.matches(catchType, state)) {
//      return false;
//    }
//
//    // Verify that the catch block is empty or contains only comments.
//    List<? extends StatementTree> catchStatements = catchTree.getBlock().getStatements();
//    for (StatementTree catchStatement : catchStatements) {
//      // Comments are not a part of the AST. Therefore, we should either get
//      // an empty list of statements (regardless of the number of comments),
//      // or a list of empty statements.
//      if (!EMPTY_STATEMENT.matches(catchStatement, state)) {
//        return false;
//      }
//    }
//
//    return true;
  }
}
