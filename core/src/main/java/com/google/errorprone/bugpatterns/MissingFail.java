/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT_AFTER_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT_BEFORE_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit4TestCases;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit4TestRunner;
import static com.google.errorprone.matchers.JUnitMatchers.isTestCaseDescendant;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.assertStatement;
import static com.google.errorprone.matchers.Matchers.assignment;
import static com.google.errorprone.matchers.Matchers.booleanConstant;
import static com.google.errorprone.matchers.Matchers.booleanLiteral;
import static com.google.errorprone.matchers.Matchers.contains;
import static com.google.errorprone.matchers.Matchers.continueStatement;
import static com.google.errorprone.matchers.Matchers.ignoreParens;
import static com.google.errorprone.matchers.Matchers.isInstanceField;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isVariable;
import static com.google.errorprone.matchers.Matchers.methodInvocation;
import static com.google.errorprone.matchers.Matchers.nextStatement;
import static com.google.errorprone.matchers.Matchers.returnStatement;
import static com.google.errorprone.matchers.Matchers.throwStatement;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.method.MethodMatchers.anyMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TryTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.ChildMultiMatcher;
import com.google.errorprone.matchers.ChildMultiMatcher.MatchType;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import javax.lang.model.element.Name;

/** @author schmitt@google.com (Peter Schmitt) */
@BugPattern(
    name = "MissingFail",
    altNames = "missing-fail",
    summary = "Not calling fail() when expecting an exception masks bugs",
    severity = WARNING)
public class MissingFail extends BugChecker implements TryTreeMatcher {

  // Many test writers don't seem to know about `fail()`. They instead use synonyms of varying
  // complexity instead.
  //
  // One category of synonyms replaces `fail()` with a single, equivalent statement
  // such as `assertTrue(false)`, `assertFalse(true)` or `throw new
  // AssertionError()`. In these cases we will simply skip any further analysis, they
  // work perfectly fine.
  //
  // Other, more complex synonyms tend to use boolean variables, like such:
  //
  // ```java
  // boolean thrown = false;
  // try {
  //   throwingExpression();
  // } catch (SomeException e) {
  //   thrown = true;
  // }
  // assertTrue(thrown);
  // ```

  private static final Matcher<ExpressionTree> ASSERT_EQUALS = Matchers.assertEqualsInvocation();
  private static final Matcher<Tree> ASSERT_UNEQUAL =
      toType(MethodInvocationTree.class, new UnequalIntegerLiteralMatcher(ASSERT_EQUALS));

  private static final Matcher<ExpressionTree> ASSERT_TRUE =
      Matchers.anyOf(
          staticMethod().onClass("org.junit.Assert").named("assertTrue"),
          staticMethod().onClass("junit.framework.Assert").named("assertTrue"),
          staticMethod().onClass("junit.framework.TestCase").named("assertTrue"));
  private static final Matcher<ExpressionTree> ASSERT_FALSE =
      Matchers.anyOf(
          staticMethod().onClass("org.junit.Assert").named("assertFalse"),
          staticMethod().onClass("junit.framework.Assert").named("assertFalse"),
          staticMethod().onClass("junit.framework.TestCase").named("assertFalse"));
  private static final Matcher<ExpressionTree> ASSERT_TRUE_FALSE =
      methodInvocation(
          ASSERT_TRUE,
          MatchType.AT_LEAST_ONE,
          Matchers.anyOf(booleanLiteral(false), booleanConstant(false)));
  private static final Matcher<ExpressionTree> ASSERT_FALSE_TRUE =
      methodInvocation(
          ASSERT_FALSE,
          MatchType.AT_LEAST_ONE,
          Matchers.anyOf(booleanLiteral(true), booleanConstant(true)));
  private static final Matcher<ExpressionTree> ASSERT_TRUE_TRUE =
      methodInvocation(
          ASSERT_TRUE,
          MatchType.AT_LEAST_ONE,
          Matchers.anyOf(booleanLiteral(true), booleanConstant(true)));
  private static final Matcher<ExpressionTree> ASSERT_FALSE_FALSE =
      methodInvocation(
          ASSERT_FALSE,
          MatchType.AT_LEAST_ONE,
          Matchers.anyOf(booleanLiteral(false), booleanConstant(false)));

  private static final Matcher<StatementTree> JAVA_ASSERT_FALSE =
      assertStatement(ignoreParens(Matchers.anyOf(booleanLiteral(false), booleanConstant(false))));

  private static final Matcher<ExpressionTree> LOG_CALL =
      anyOf(
          instanceMethod()
              .onClass((t, s) -> t.asElement().getSimpleName().toString().contains("Logger"))
              .withAnyName(),
          instanceMethod().anyClass().withNameMatching(Pattern.compile("log.*")));
  private static final Matcher<Tree> LOG_IN_BLOCK =
      contains(toType(ExpressionTree.class, LOG_CALL));

  private static final Pattern FAIL_PATTERN = Pattern.compile(".*(?i:fail).*");
  private static final Matcher<ExpressionTree> FAIL =
      anyMethod().anyClass().withNameMatching(FAIL_PATTERN);

  private static final Matcher<ExpressionTree> ASSERT_CALL =
      methodInvocation(new AssertMethodMatcher());
  private static final Matcher<ExpressionTree> REAL_ASSERT_CALL =
      Matchers.allOf(
          ASSERT_CALL, Matchers.not(Matchers.anyOf(ASSERT_FALSE_FALSE, ASSERT_TRUE_TRUE)));
  private static final Matcher<ExpressionTree> VERIFY_CALL =
      staticMethod().onClass("org.mockito.Mockito").named("verify");
  private static final MultiMatcher<TryTree, Tree> ASSERT_LAST_CALL_IN_TRY =
      new ChildOfTryMatcher(
          MatchType.LAST,
          contains(toType(ExpressionTree.class, Matchers.anyOf(REAL_ASSERT_CALL, VERIFY_CALL))));
  private static final Matcher<Tree> ASSERT_IN_BLOCK =
      contains(toType(ExpressionTree.class, REAL_ASSERT_CALL));

  private static final Matcher<StatementTree> THROW_STATEMENT = throwStatement(Matchers.anything());
  private static final Matcher<Tree> THROW_OR_FAIL_IN_BLOCK =
      contains(
          Matchers.anyOf(
              toType(StatementTree.class, THROW_STATEMENT),
              // TODO(schmitt): Include Preconditions.checkState(false)?
              toType(ExpressionTree.class, ASSERT_TRUE_FALSE),
              toType(ExpressionTree.class, ASSERT_FALSE_TRUE),
              toType(ExpressionTree.class, ASSERT_UNEQUAL),
              toType(StatementTree.class, JAVA_ASSERT_FALSE),
              toType(ExpressionTree.class, FAIL)));

  private static final Matcher<TryTree> NON_TEST_METHOD = new IgnoredEnclosingMethodMatcher();

  private static final Matcher<Tree> RETURN_IN_BLOCK =
      contains(toType(StatementTree.class, returnStatement(Matchers.anything())));
  private static final Matcher<StatementTree> RETURN_AFTER =
      nextStatement(returnStatement(Matchers.anything()));

  private static final Matcher<VariableTree> INAPPLICABLE_EXCEPTION =
      Matchers.anyOf(
          isSameType("java.lang.InterruptedException"),
          isSameType("java.lang.AssertionError"),
          isSameType("java.lang.Throwable"),
          isSameType("junit.framework.AssertionFailedError"));

  private static final InLoopMatcher IN_LOOP = new InLoopMatcher();

  private static final Matcher<Tree> WHILE_TRUE_IN_BLOCK =
      contains(toType(WhileLoopTree.class, new WhileTrueLoopMatcher()));

  private static final Matcher<Tree> CONTINUE_IN_BLOCK =
      contains(toType(StatementTree.class, continueStatement()));

  private static final Matcher<AssignmentTree> FIELD_ASSIGNMENT =
      assignment(isInstanceField(), Matchers.<ExpressionTree>anything());
  private static final Matcher<Tree> FIELD_ASSIGNMENT_IN_BLOCK =
      contains(toType(AssignmentTree.class, FIELD_ASSIGNMENT));

  private static final Matcher<ExpressionTree> BOOLEAN_ASSERT_VAR =
      methodInvocation(
          Matchers.anyOf(ASSERT_FALSE, ASSERT_TRUE),
          MatchType.AT_LEAST_ONE,
          Matchers.anyOf(isInstanceField(), isVariable()));
  private static final Matcher<Tree> BOOLEAN_ASSERT_VAR_IN_BLOCK =
      contains(toType(ExpressionTree.class, BOOLEAN_ASSERT_VAR));

  // Subtly different from JUnitMatchers: We want to match test base classes too.
  private static final Matcher<ClassTree> TEST_CLASS =
      Matchers.anyOf(isTestCaseDescendant, hasJUnit4TestRunner, hasJUnit4TestCases);

  @Override
  public Description matchTry(TryTree tree, VisitorState state) {
    if (tryTreeMatches(tree, state)) {
      List<? extends StatementTree> tryStatements = tree.getBlock().getStatements();
      StatementTree lastTryStatement = tryStatements.get(tryStatements.size() - 1);

      Optional<Fix> assertThrowsFix =
          AssertThrowsUtils.tryFailToAssertThrows(tree, tryStatements, Optional.empty(), state);
      Fix failFix = addFailCall(tree, lastTryStatement, state);
      return buildDescription(lastTryStatement).addFix(assertThrowsFix).addFix(failFix).build();
    } else {
      return Description.NO_MATCH;
    }
  }

  public static Fix addFailCall(TryTree tree, StatementTree lastTryStatement, VisitorState state) {
    String failCall = String.format("\nfail(\"Expected %s\");", exceptionToString(tree, state));
    SuggestedFix.Builder fixBuilder =
        SuggestedFix.builder().postfixWith(lastTryStatement, failCall);

    // Make sure that when the fail import is added it doesn't conflict with existing ones.
    fixBuilder.removeStaticImport("junit.framework.Assert.fail");
    fixBuilder.removeStaticImport("junit.framework.TestCase.fail");
    fixBuilder.addStaticImport("org.junit.Assert.fail");

    return fixBuilder.build();
  }

  /**
   * Returns a string describing the exception type caught by the given try tree's catch
   * statement(s), defaulting to {@code "Exception"} if more than one exception type is caught.
   */
  private static String exceptionToString(TryTree tree, VisitorState state) {
    if (tree.getCatches().size() != 1) {
      return "Exception";
    }
    Tree exceptionType = tree.getCatches().iterator().next().getParameter().getType();
    Type type = ASTHelpers.getType(exceptionType);
    if (type != null && type.isUnion()) {
      return "Exception";
    }
    return state.getSourceForNode(exceptionType);
  }

  private boolean tryTreeMatches(TryTree tree, VisitorState state) {
    if (!isInClass(tree, state, TEST_CLASS)) {
      return false;
    }

    if (hasToleratedException(tree)) {
      return false;
    }

    boolean assertInCatch = hasAssertInCatch(tree, state);
    if (!hasExpectedException(tree) && !assertInCatch) {
      return false;
    }

    if (hasThrowOrFail(tree, state)
        || isInInapplicableMethod(tree, state)
        || returnsInTryCatchOrAfter(tree, state)
        || isInapplicableExceptionType(tree, state)
        || isInLoop(state, tree)
        || hasWhileTrue(tree, state)
        || hasContinue(tree, state)
        || hasFinally(tree)
        || logsInCatch(state, tree)) {
      return false;
    }

    if (assertInCatch
        && (hasFieldAssignmentInCatch(tree, state)
            || hasBooleanAssertVariableInCatch(tree, state)
            || lastTryStatementIsAssert(tree, state))) {
      return false;
    }

    if (tree.getBlock().getStatements().isEmpty()) {
      return false;
    }

    return true;
  }

  private boolean hasWhileTrue(TryTree tree, VisitorState state) {
    return WHILE_TRUE_IN_BLOCK.matches(tree, state);
  }

  private boolean isInClass(TryTree tree, VisitorState state, Matcher<ClassTree> classTree) {
    return Matchers.enclosingNode(toType(ClassTree.class, classTree)).matches(tree, state);
  }

  private boolean hasBooleanAssertVariableInCatch(TryTree tree, VisitorState state) {
    return anyCatchBlockMatches(tree, state, BOOLEAN_ASSERT_VAR_IN_BLOCK);
  }

  private boolean lastTryStatementIsAssert(TryTree tree, VisitorState state) {
    return ASSERT_LAST_CALL_IN_TRY.matches(tree, state);
  }

  private boolean hasFieldAssignmentInCatch(TryTree tree, VisitorState state) {
    return anyCatchBlockMatches(tree, state, FIELD_ASSIGNMENT_IN_BLOCK);
  }

  private boolean logsInCatch(VisitorState state, TryTree tree) {
    return anyCatchBlockMatches(tree, state, LOG_IN_BLOCK);
  }

  private boolean hasFinally(TryTree tree) {
    return tree.getFinallyBlock() != null;
  }

  private boolean hasContinue(TryTree tree, VisitorState state) {
    return CONTINUE_IN_BLOCK.matches(tree, state);
  }

  private boolean isInLoop(VisitorState state, TryTree tree) {
    return IN_LOOP.matches(tree, state);
  }

  private boolean isInapplicableExceptionType(TryTree tree, VisitorState state) {
    for (CatchTree catchTree : tree.getCatches()) {
      if (INAPPLICABLE_EXCEPTION.matches(catchTree.getParameter(), state)) {
        return true;
      }
    }
    return false;
  }

  private boolean returnsInTryCatchOrAfter(TryTree tree, VisitorState state) {
    return RETURN_IN_BLOCK.matches(tree, state) || RETURN_AFTER.matches(tree, state);
  }

  private boolean isInInapplicableMethod(TryTree tree, VisitorState state) {
    return NON_TEST_METHOD.matches(tree, state);
  }

  private boolean hasThrowOrFail(TryTree tree, VisitorState state) {
    return THROW_OR_FAIL_IN_BLOCK.matches(tree, state);
  }

  private boolean hasAssertInCatch(TryTree tree, VisitorState state) {
    return anyCatchBlockMatches(tree, state, ASSERT_IN_BLOCK);
  }

  private boolean hasToleratedException(TryTree tree) {
    for (CatchTree catchTree : tree.getCatches()) {
      if (catchTree.getParameter().getName().contentEquals("tolerated")) {
        return true;
      }
    }
    return false;
  }

  private boolean hasExpectedException(TryTree tree) {
    for (CatchTree catchTree : tree.getCatches()) {
      if (catchTree.getParameter().getName().contentEquals("expected")) {
        return true;
      }
    }
    return false;
  }

  private boolean anyCatchBlockMatches(TryTree tree, VisitorState state, Matcher<Tree> matcher) {
    for (CatchTree catchTree : tree.getCatches()) {
      if (matcher.matches(catchTree.getBlock(), state)) {
        return true;
      }
    }
    return false;
  }

  private static class AssertMethodMatcher implements Matcher<ExpressionTree> {

    @Override
    public boolean matches(ExpressionTree expressionTree, VisitorState state) {
      Symbol sym = ASTHelpers.getSymbol(expressionTree);

      if (sym == null) {
        return false;
      }

      String symSimpleName = sym.getSimpleName().toString();
      return symSimpleName.startsWith("assert") || symSimpleName.startsWith("verify");
    }
  }

  /** Matches any try-tree that is enclosed in a loop. */
  private static class InLoopMatcher implements Matcher<TryTree> {

    @Override
    public boolean matches(TryTree tryTree, VisitorState state) {
      return ASTHelpers.findEnclosingNode(state.getPath(), DoWhileLoopTree.class) != null
          || ASTHelpers.findEnclosingNode(state.getPath(), EnhancedForLoopTree.class) != null
          || ASTHelpers.findEnclosingNode(state.getPath(), WhileLoopTree.class) != null
          || ASTHelpers.findEnclosingNode(state.getPath(), ForLoopTree.class) != null;
    }
  }

  private static class WhileTrueLoopMatcher implements Matcher<WhileLoopTree> {

    @Override
    public boolean matches(WhileLoopTree tree, VisitorState state) {
      return ignoreParens(booleanLiteral(true)).matches(tree.getCondition(), state);
    }
  }

  /**
   * Matches any try-tree that is in a JUNit3 {@code setUp} or {@code tearDown} method, their JUnit4
   * equivalents, a JUnit {@code suite()} method or a {@code main} method.
   */
  private static class IgnoredEnclosingMethodMatcher implements Matcher<TryTree> {

    @Override
    public boolean matches(TryTree tryTree, VisitorState state) {
      MethodTree enclosingMethodTree =
          ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
      if (enclosingMethodTree == null) {
        // e.g. a class initializer
        return true;
      }
      Name name = enclosingMethodTree.getName();
      return JUnitMatchers.looksLikeJUnit3SetUp.matches(enclosingMethodTree, state)
          || JUnitMatchers.looksLikeJUnit3TearDown.matches(enclosingMethodTree, state)
          || name.contentEquals("main")
          // TODO(schmitt): Move to JUnitMatchers?
          || name.contentEquals("suite")
          || Matchers.hasAnnotation(JUNIT_BEFORE_ANNOTATION).matches(enclosingMethodTree, state)
          || Matchers.hasAnnotation(JUNIT_AFTER_ANNOTATION).matches(enclosingMethodTree, state);
    }
  }

  /**
   * Matches if any two of the given list of expressions are integer literals with different values.
   */
  private static class UnequalIntegerLiteralMatcher implements Matcher<MethodInvocationTree> {

    private final Matcher<ExpressionTree> methodSelectMatcher;

    private UnequalIntegerLiteralMatcher(Matcher<ExpressionTree> methodSelectMatcher) {
      this.methodSelectMatcher = methodSelectMatcher;
    }

    @Override
    public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
      return methodSelectMatcher.matches(methodInvocationTree, state)
          && matches(methodInvocationTree.getArguments());
    }

    private boolean matches(List<? extends ExpressionTree> expressionTrees) {
      Set<Integer> foundValues = new HashSet<>();
      for (Tree tree : expressionTrees) {
        if (tree instanceof LiteralTree) {
          Object value = ((LiteralTree) tree).getValue();
          if (value instanceof Integer) {
            boolean duplicate = !foundValues.add((Integer) value);
            if (!duplicate && foundValues.size() > 1) {
              return true;
            }
          }
        }
      }
      return false;
    }
  }

  private static class ChildOfTryMatcher extends ChildMultiMatcher<TryTree, Tree> {

    public ChildOfTryMatcher(MatchType matchType, Matcher<Tree> nodeMatcher) {
      super(matchType, nodeMatcher);
    }

    @Override
    protected Iterable<? extends StatementTree> getChildNodes(TryTree tree, VisitorState state) {
      return tree.getBlock().getStatements();
    }
  }
}
