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
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.expressionMethodSelect;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.sun.source.tree.Tree.Kind.EMPTY_STATEMENT;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

import java.util.List;

/**
 * A bug checker for the following code pattern:
 *
 * <pre>
 * try {
 *   // do something
 *   Assert.fail(); // or any Assert.assert*
 *   // maybe do something more
 * } catch (Throwable t) {
 *   // empty or only comments
 * }
 * </pre>
 * *
 * Matches all static methods named "fail" and starting with "assert" from
 * the following classes:
 * <ul>
 * <li>{@code org.junit.Assert},
 * <li>{@code junit.framework.Assert},
 * <li>{@code junit.framework.TestCase} - which overrides the methods from Assert in order to
 * deprecate them,
 * <li>{@code com.google.testing.util.MoreAsserts} and
 * <li>every class whose name ends with "MoreAsserts".
 * </ul>
 *
 * Possible improvements/generalizations of this matcher:
 * <ul>
 * <li>catching Error (suggesting a fix may be tricky; will need to add an assert to the catch
 *     that will assert the caught error is not an assertion failed)
 * <li>support multiple catch() blocks
 * <li>support MoreAsserts
 * </ul>
 *
 * @author adamwos@google.com (Adam Wos)
 */
@BugPattern(name = "TryFailThrowable",
    summary = "Catching Throwable masks failures from fail() or assert*() in the try block",
    explanation = "When testing that a line of code throws an expected exception, it is "
        + "typical to execute that line in a try block with a `fail()` or `assert*()` on the "
        + "line following.  The expectation is that the expected exception will be thrown, and "
        + "execution will continue in the catch block, and the `fail()` or `assert*()` will not "
        + "be executed.\n\n"
        + "`fail()` and `assert*()` throw AssertionErrors, which are a subtype of Throwable. "
        + "That means that if if the catch block catches Throwable, then execution will "
        + "always jump to the catch block, and the test will always pass.\n\n"
        + "To fix this, you usually want to catch Exception rather than Throwable. If you need "
        + "to catch throwable (e.g., the expected exception is an AssertionError), then add logic "
        + "in your catch block to ensure that the AssertionError that was caught is not the same "
        + "one thrown by the call to `fail()` or `assert*()`.",
    category = JUNIT, maturity = MATURE, severity = ERROR)
public class TryFailThrowable extends BugChecker implements TryTreeMatcher {

  private static final Matcher<VariableTree> javaLangThrowable = isSameType("java.lang.Throwable");

  private static final Matcher<ExpressionTree> failOrAssert = expressionMethodSelect(
      new Matcher<ExpressionTree>() {
        @Override public boolean matches(ExpressionTree item, VisitorState state) {
          Symbol sym = ASTHelpers.getSymbol(item);
          if (!(sym instanceof MethodSymbol)) {
            throw new IllegalArgumentException("not a method call");
          }
          if (!sym.isStatic()) {
            return false;
          }

          String methodName = sym.getQualifiedName().toString();
          String className = sym.owner.getQualifiedName().toString();
          return (methodName.startsWith("assert") || methodName.startsWith("fail"))
              && (className.equals("org.junit.Assert")
                  || className.equals("junit.framework.Assert")
                  || className.equals("junit.framework.TestCase")
                  || className.endsWith("MoreAsserts"));
        }
      });

  @Override
  public Description matchTry(TryTree tree, VisitorState state) {
    if (tryTreeMatches(tree, state)) {
      CatchTree firstCatch = tree.getCatches().get(0);
      VariableTree catchParameter = firstCatch.getParameter();
      return describeMatch(firstCatch,
          SuggestedFix.replace(catchParameter, "Exception " + catchParameter.getName()));
    } else {
      return Description.NO_MATCH;
    }
  }

  private boolean tryTreeMatches(TryTree tryTree, VisitorState state) {
    BlockTree tryBlock = tryTree.getBlock();
    List<? extends StatementTree> statements = tryBlock.getStatements();
    if (statements.isEmpty()) {
      return false;
    }

    // Check if any of the statements is a fail or assert* method (i.e. any
    // method that can throw an AssertionFailedError)
    boolean foundFailOrAssert = false;
    for (StatementTree statement : statements) {
      if (!(statement instanceof ExpressionStatementTree)) {
        continue;
      }
      if (failOrAssert.matches(
          ((ExpressionStatementTree) statement).getExpression(), state)) {
        foundFailOrAssert = true;
        break;
      }
    }
    if (!foundFailOrAssert) {
      return false;
    }

    // Verify that the only catch clause catches Throwable
    List<? extends CatchTree> catches = tryTree.getCatches();
    if (catches.size() != 1) {
      // TODO(user): this could be supported - only the last catch would need
      // to be checked - it would either be Throwable or Error.
      return false;
    }
    CatchTree catchTree = catches.get(0);
    VariableTree catchType = catchTree.getParameter();
    if (!javaLangThrowable.matches(catchType, state)) {
      // TODO(user): Error could be supported
      return false;
    }

    // Verify that the catch block is empty or contains only comments.
    List<? extends StatementTree> catchStatements = catchTree.getBlock().getStatements();
    for (StatementTree catchStatement : catchStatements) {
      // Comments are not a part of the AST. Therefore, we should either get
      // an empty list of statements (regardless of the number of comments),
      // or a list of empty statements.
      if (!Matchers.<Tree>kindIs(EMPTY_STATEMENT).matches(catchStatement, state)) {
        return false;
      }
    }

    return true;
  }
}
