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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.Category.JUNIT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.TryFailThrowable.CaughtType.JAVA_LANG_ERROR;
import static com.google.errorprone.bugpatterns.TryFailThrowable.CaughtType.JAVA_LANG_THROWABLE;
import static com.google.errorprone.bugpatterns.TryFailThrowable.CaughtType.SOME_ASSERTION_FAILURE;
import static com.google.errorprone.bugpatterns.TryFailThrowable.MatchResult.doesNotMatch;
import static com.google.errorprone.bugpatterns.TryFailThrowable.MatchResult.matches;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.sun.source.tree.Tree.Kind.BLOCK;
import static com.sun.source.tree.Tree.Kind.EMPTY_STATEMENT;
import static com.sun.source.tree.Tree.Kind.METHOD;
import static com.sun.source.tree.Tree.Kind.METHOD_INVOCATION;
import static java.lang.String.format;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TryTreeMatcher;
import com.google.errorprone.fixes.Fix;
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
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Types;
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
 *
 * * Matches all static methods named "fail" and starting with "assert" from the following classes:
 *
 * <ul>
 *   <li>{@code org.junit.Assert},
 *   <li>{@code junit.framework.Assert},
 *   <li>{@code junit.framework.TestCase} - which overrides the methods from Assert in order to
 *       deprecate them,
 *   <li>{@code com.google.testing.util.MoreAsserts} and
 *   <li>every class whose name ends with "MoreAsserts".
 * </ul>
 *
 * Possible improvements/generalizations of this matcher:
 *
 * <ul>
 *   <li>support multiple catch() blocks
 *   <li>support MoreAsserts
 * </ul>
 *
 * @author adamwos@google.com (Adam Wos)
 */
@BugPattern(
  name = "TryFailThrowable",
  summary = "Catching Throwable/Error masks failures from fail() or assert*() in the try block",
  explanation =
      "When testing that a line of code throws an expected exception, it is "
          + "typical to execute that line in a try block with a `fail()` or `assert*()` on the "
          + "line following.  The expectation is that the expected exception will be thrown, and "
          + "execution will continue in the catch block, and the `fail()` or `assert*()` will not "
          + "be executed.\n\n"
          + "`fail()` and `assert*()` throw AssertionErrors, which are a subtype of Throwable. "
          + "That means that if if the catch block catches Throwable, then execution will "
          + "always jump to the catch block, and the test will always pass.\n\n"
          + "To fix this, you usually want to catch Exception rather than Throwable. If you need "
          + "to catch throwable (e.g., the expected exception is an AssertionError), then add "
          + "logic in your catch block to ensure that the AssertionError that was caught is not "
          + "the same one thrown by the call to `fail()` or `assert*()`.",
  category = JUNIT,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class TryFailThrowable extends BugChecker implements TryTreeMatcher {

  private static final Matcher<VariableTree> javaLangThrowable = isSameType("java.lang.Throwable");
  private static final Matcher<VariableTree> javaLangError = isSameType("java.lang.Error");
  private static final Matcher<VariableTree> someAssertionFailure =
      anyOf(
          isSameType("java.lang.AssertionError"),
          isSameType("junit.framework.AssertionFailedError"));

  private static final Matcher<ExpressionTree> failOrAssert =
      new Matcher<ExpressionTree>() {
        @Override
        public boolean matches(ExpressionTree item, VisitorState state) {
          if (item.getKind() != METHOD_INVOCATION) {
            return false;
          }
          Symbol sym = getSymbol(item);
          if (!(sym instanceof MethodSymbol)) {
            throw new IllegalArgumentException("not a method call");
          }
          if (!sym.isStatic()) {
            return false;
          }

          String methodName = sym.getQualifiedName().toString();
          String className = sym.owner.getQualifiedName().toString();
          // TODO(cpovirk): Look for literal "throw new AssertionError()," etc.
          return (methodName.startsWith("assert") || methodName.startsWith("fail"))
              && (className.equals("org.junit.Assert")
                  || className.equals("junit.framework.Assert")
                  || className.equals("junit.framework.TestCase")
                  || className.endsWith("MoreAsserts"));
        }
      };

  @Override
  public Description matchTry(TryTree tree, VisitorState state) {
    MatchResult matchResult = tryTreeMatches(tree, state);
    if (!matchResult.matched()) {
      return NO_MATCH;
    }

    Description.Builder builder = buildDescription(tree.getCatches().get(0).getParameter());
    if (matchResult.caughtType == JAVA_LANG_THROWABLE) {
      builder.addFix(fixByCatchingException(tree));
    }
    if (matchResult.caughtType == SOME_ASSERTION_FAILURE) {
      builder.addFix(fixByThrowingJavaLangError(matchResult.failStatement, state));
    }
    builder.addFix(fixWithReturnOrBoolean(tree, matchResult.failStatement, state));
    return builder.build();
  }

  private static Fix fixByCatchingException(TryTree tryTree) {
    VariableTree catchParameter = getOnlyCatch(tryTree).getParameter();
    return replace(catchParameter, "Exception " + catchParameter.getName());
  }

  private static Fix fixByThrowingJavaLangError(StatementTree failStatement, VisitorState state) {
    String messageSnippet = getMessageSnippet(failStatement, state, HasOtherParameters.FALSE);
    return replace(failStatement, format("throw new Error(%s);", messageSnippet));
  }

  private static Fix fixWithReturnOrBoolean(
      TryTree tryTree, StatementTree failStatement, VisitorState state) {
    Tree parent = state.getPath().getParentPath().getLeaf();
    Tree grandparent = state.getPath().getParentPath().getParentPath().getLeaf();
    if (parent.getKind() == BLOCK
        && grandparent.getKind() == METHOD
        && tryTree == getLastStatement((BlockTree) parent)) {
      return fixWithReturn(tryTree, failStatement, state);
    } else {
      return fixWithBoolean(tryTree, failStatement, state);
    }
  }

  private static Fix fixWithReturn(
      TryTree tryTree, StatementTree failStatement, VisitorState state) {
    SuggestedFix.Builder builder = SuggestedFix.builder();
    builder.delete(failStatement);
    builder.replace(getOnlyCatch(tryTree).getBlock(), "{ return; }");
    // TODO(cpovirk): Use the file's preferred assertion API.
    String messageSnippet = getMessageSnippet(failStatement, state, HasOtherParameters.FALSE);
    builder.postfixWith(tryTree, format("fail(%s);", messageSnippet));
    return builder.build();
  }

  private static Fix fixWithBoolean(
      TryTree tryTree, StatementTree failStatement, VisitorState state) {
    SuggestedFix.Builder builder = SuggestedFix.builder();
    builder.delete(failStatement);
    builder.prefixWith(tryTree, "boolean threw = false;");
    builder.replace(getOnlyCatch(tryTree).getBlock(), "{ threw = true; }");
    // TODO(cpovirk): Use the file's preferred assertion API.
    String messageSnippet = getMessageSnippet(failStatement, state, HasOtherParameters.TRUE);
    builder.postfixWith(tryTree, format("assertTrue(%sthrew);", messageSnippet));
    return builder.build();
  }

  private static String getMessageSnippet(
      StatementTree failStatement, VisitorState state, HasOtherParameters hasOtherParameters) {
    ExpressionTree expression = ((ExpressionStatementTree) failStatement).getExpression();
    MethodSymbol sym = (MethodSymbol) getSymbol(expression);
    String tail = hasOtherParameters == HasOtherParameters.TRUE ? ", " : "";
    // The above casts were checked earlier by failOrAssert.
    return hasInitialStringParameter(sym, state)
        ? state.getSourceForNode(((MethodInvocationTree) expression).getArguments().get(0)) + tail
        : "";
  }

  /**
   * Whether the assertion method we're inserting a call to has extra parameters besides its message
   * (like {@code assertTrue}) or not (like {@code fail}).
   */
  enum HasOtherParameters {
    TRUE,
    FALSE;
  }

  private static boolean hasInitialStringParameter(MethodSymbol sym, VisitorState state) {
    Types types = state.getTypes();
    List<VarSymbol> parameters = sym.getParameters();
    return !parameters.isEmpty()
        && types.isSameType(parameters.get(0).type, state.getSymtab().stringType);
  }

  private static MatchResult tryTreeMatches(TryTree tryTree, VisitorState state) {
    BlockTree tryBlock = tryTree.getBlock();
    List<? extends StatementTree> statements = tryBlock.getStatements();
    if (statements.isEmpty()) {
      return doesNotMatch();
    }

    // Check if any of the statements is a fail or assert* method (i.e. any
    // method that can throw an AssertionFailedError)
    StatementTree failStatement = null;
    for (StatementTree statement : statements) {
      if (!(statement instanceof ExpressionStatementTree)) {
        continue;
      }
      if (failOrAssert.matches(((ExpressionStatementTree) statement).getExpression(), state)) {
        failStatement = statement;
        break;
      }
    }
    if (failStatement == null) {
      return doesNotMatch();
    }

    // Verify that the only catch clause catches Throwable
    List<? extends CatchTree> catches = tryTree.getCatches();
    if (catches.size() != 1) {
      // TODO(adamwos): this could be supported - only the last catch would need
      // to be checked - it would either be Throwable or Error.
      return doesNotMatch();
    }
    CatchTree catchTree = catches.get(0);
    VariableTree catchType = catchTree.getParameter();
    boolean catchesThrowable = javaLangThrowable.matches(catchType, state);
    boolean catchesError = javaLangError.matches(catchType, state);
    boolean catchesOtherError = someAssertionFailure.matches(catchType, state);
    if (!catchesThrowable && !catchesError && !catchesOtherError) {
      return doesNotMatch();
    }

    // Verify that the catch block is empty or contains only comments.
    List<? extends StatementTree> catchStatements = catchTree.getBlock().getStatements();
    for (StatementTree catchStatement : catchStatements) {
      // Comments are not a part of the AST. Therefore, we should either get
      // an empty list of statements (regardless of the number of comments),
      // or a list of empty statements.
      if (!Matchers.<Tree>kindIs(EMPTY_STATEMENT).matches(catchStatement, state)) {
        return doesNotMatch();
      }
    }

    return matches(
        failStatement,
        catchesThrowable
            ? JAVA_LANG_THROWABLE
            : catchesError ? JAVA_LANG_ERROR : SOME_ASSERTION_FAILURE);
  }

  static final class MatchResult {
    static final MatchResult DOES_NOT_MATCH = new MatchResult(null, null);

    static MatchResult matches(StatementTree failStatement, CaughtType caughtType) {
      return new MatchResult(checkNotNull(failStatement), checkNotNull(caughtType));
    }

    static MatchResult doesNotMatch() {
      return DOES_NOT_MATCH;
    }

    final StatementTree failStatement;
    final CaughtType caughtType;

    MatchResult(StatementTree failStatement, CaughtType caughtType) {
      this.failStatement = failStatement;
      this.caughtType = caughtType;
    }

    boolean matched() {
      return caughtType != null;
    }
  }

  enum CaughtType {
    JAVA_LANG_ERROR,
    JAVA_LANG_THROWABLE,
    SOME_ASSERTION_FAILURE,
    ;
  }

  private static StatementTree getLastStatement(BlockTree blockTree) {
    return getLast(blockTree.getStatements());
  }

  private static CatchTree getOnlyCatch(TryTree tryTree) {
    return tryTree.getCatches().get(0);
  }
}
