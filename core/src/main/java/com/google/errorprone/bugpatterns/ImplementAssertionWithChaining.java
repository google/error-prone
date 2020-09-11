/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;
import static com.sun.source.tree.Tree.Kind.BLOCK;
import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static com.sun.source.tree.Tree.Kind.MEMBER_SELECT;
import static com.sun.source.tree.Tree.Kind.METHOD_INVOCATION;
import static java.lang.String.format;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.UnaryTree;
import com.sun.tools.javac.code.Symbol;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Migrates Truth subjects from a manual "test and fail" approach to one using {@code
 * Subject.check(...)}. For example:
 *
 * <pre>{@code
 * // Before:
 * if (actual().foo() != expected) {
 *   failWithActual("expected to have foo", expected);
 * }
 *
 * // After:
 * check("foo()").that(actual().foo()).isEqualTo(expected);
 * }</pre>
 */
@BugPattern(
    name = "ImplementAssertionWithChaining",
    summary = "Prefer check(...), which usually generates more readable failure messages.",
    severity = SUGGESTION)
public final class ImplementAssertionWithChaining extends BugChecker implements IfTreeMatcher {
  @Override
  public Description matchIf(IfTree ifTree, VisitorState state) {
    if (ifTree.getElseStatement() != null) {
      return NO_MATCH;
    }

    if (!isCallToFail(ifTree.getThenStatement(), state)) {
      return NO_MATCH;
    }

    /*
     * TODO(cpovirk): Also look for assertions that could use isTrue, isFalse, isNull, isEmpty, etc.
     * (But isTrue and isFalse in particular often benefit from custom messages.)
     */
    ImmutableList<ExpressionTree> actualAndExpected =
        findActualAndExpected(stripParentheses(ifTree.getCondition()), state);
    if (actualAndExpected == null) {
      return NO_MATCH;
    }

    String checkDescription = makeCheckDescription(actualAndExpected.get(0), state);
    if (checkDescription == null) {
      return NO_MATCH;
    }

    /*
     * TODO(cpovirk): Write "this.check(...)" if the code uses "this.actual()" or "this.fail*(...)."
     * Similarly for "otherObject.check(...)."
     */
    return describeMatch(
        ifTree,
        replace(
            ifTree,
            format(
                "check(%s).that(%s).isEqualTo(%s);",
                checkDescription,
                state.getSourceForNode(actualAndExpected.get(0)),
                state.getSourceForNode(actualAndExpected.get(1)))));
  }

  private static ImmutableList<ExpressionTree> findActualAndExpected(
      ExpressionTree condition, VisitorState state) {
    /*
     * Note that all these look "backward": If the code is "if (foo == bar) { fail }," then the
     * assertion is checking that the values are *not* equal.
     */
    switch (condition.getKind()) {
      case LOGICAL_COMPLEMENT:
        return findActualAndExpectedForPossibleEqualsCall(
            stripParentheses(((UnaryTree) condition).getExpression()), state);

      case NOT_EQUAL_TO:
        return findActualAndExpectedForBinaryOp((BinaryTree) condition, state);

      default:
        return null;
    }
  }

  private static ImmutableList<ExpressionTree> findActualAndExpectedForPossibleEqualsCall(
      ExpressionTree possiblyEqualsCall, VisitorState state) {
    if (!EQUALS_LIKE_METHOD.matches(possiblyEqualsCall, state)) {
      return null;
    }

    MethodInvocationTree equalsCheck = (MethodInvocationTree) possiblyEqualsCall;
    List<? extends ExpressionTree> args = equalsCheck.getArguments();
    return (args.size() == 2)
        ? ImmutableList.copyOf(args)
        : ImmutableList.of(
            ((MemberSelectTree) equalsCheck.getMethodSelect()).getExpression(),
            getOnlyElement(args));
  }

  private static ImmutableList<ExpressionTree> findActualAndExpectedForBinaryOp(
      BinaryTree binaryTree, VisitorState state) {
    /*
     * It's actually enough for *either* to be a primitive, thanks to autounboxing (and enough for
     * *either* to be an enum, since equals() is symmetric). However, it turns out that handling
     * those cases catches almost nothing new in practice, and I'm seeing some evidence that "null"
     * is considered to be a primitive? or something? That seems wrong, but given the low payoff,
     * I'm not going to investigate further.
     */
    boolean bothPrimitives =
        getType(binaryTree.getLeftOperand()).isPrimitive()
            && getType(binaryTree.getRightOperand()).isPrimitive();
    boolean bothEnums =
        isEnum(binaryTree.getLeftOperand(), state) && isEnum(binaryTree.getRightOperand(), state);
    if (!bothPrimitives && !bothEnums) {
      // TODO(cpovirk): Generate an isSameAs() check (if that is what users really want).
      return null;
    }
    return ImmutableList.of(binaryTree.getLeftOperand(), binaryTree.getRightOperand());
  }

  private static boolean isEnum(ExpressionTree tree, VisitorState state) {
    return isSubtype(getType(tree), state.getSymtab().enumSym.type, state);
  }

  /**
   * Checks that the statement, after unwrapping any braces, consists of a single call to a {@code
   * fail*} method.
   */
  private static boolean isCallToFail(StatementTree then, VisitorState state) {
    while (then.getKind() == BLOCK) {
      List<? extends StatementTree> statements = ((BlockTree) then).getStatements();
      if (statements.size() != 1) {
        return false;
      }
      then = getOnlyElement(statements);
    }
    if (then.getKind() != EXPRESSION_STATEMENT) {
      return false;
    }
    ExpressionTree thenExpr = ((ExpressionStatementTree) then).getExpression();
    if (thenExpr.getKind() != METHOD_INVOCATION) {
      return false;
    }
    MethodInvocationTree thenCall = (MethodInvocationTree) thenExpr;
    ExpressionTree methodSelect = thenCall.getMethodSelect();
    if (methodSelect.getKind() != IDENTIFIER) {
      return false;
      // TODO(cpovirk): Handle "this.fail*(...)," etc.
    }
    return FAIL_METHOD.matches(methodSelect, state);
  }

  /**
   * Converts the tree for the actual value under test (like {@code actual().foo()}) to a string
   * suitable for passing to {@code Subject.check(...)} (like {@code "foo()"}, which Truth appends
   * to the name is has for the actual value, producing something like {@code "bar.foo()"}).
   *
   * <p>Sometimes the tree contains multiple method calls, like {@code actual().foo().bar()}. In
   * that case, they appear "backward" as we walk the tree (i.e., bar, foo), so we add each one to
   * the beginning of the list as we go.
   */
  static String makeCheckDescription(ExpressionTree actual, VisitorState state) {
    /*
     * This conveniently also acts as a check that the actual and expected values aren't backward,
     * since the actual value is almost always an invocation on actual() and the expected value is
     * almost always a parameter.
     */
    if (actual.getKind() != METHOD_INVOCATION) {
      return null;
    }

    Deque<String> parts = new ArrayDeque<>();
    MethodInvocationTree invocation = (MethodInvocationTree) actual;
    while (true) {
      ExpressionTree methodSelect = invocation.getMethodSelect();
      if (methodSelect.getKind() != MEMBER_SELECT) {
        return null;
      }
      MemberSelectTree memberSelect = (MemberSelectTree) methodSelect;

      if (!invocation.getArguments().isEmpty()) {
        // TODO(cpovirk): Handle invocations with arguments.
        return null;
      }
      parts.addFirst(memberSelect.getIdentifier() + "()");

      ExpressionTree expression = memberSelect.getExpression();
      if (ACTUAL_METHOD.matches(expression, state) || refersToFieldNamedActual(expression)) {
        return '"' + Joiner.on('.').join(parts) + '"';
      }
      if (expression.getKind() != METHOD_INVOCATION) {
        return null;
      }
      invocation = (MethodInvocationTree) expression;
    }
  }

  private static boolean refersToFieldNamedActual(ExpressionTree tree) {
    Symbol symbol = getSymbol(tree);
    // Using the name "actual" for this field is just a convention, but that's good enough here.
    return symbol != null
        && symbol.getKind().isField()
        && symbol.getSimpleName().contentEquals("actual");
  }

  private static final Matcher<ExpressionTree> FAIL_METHOD =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.Subject")
          .withNameMatching(Pattern.compile("fail.*"));

  private static final Matcher<ExpressionTree> EQUALS_LIKE_METHOD =
      anyOf(
          instanceMethod().anyClass().named("equals").withParameters("java.lang.Object"),
          staticMethod().onClass("com.google.common.base.Objects").named("equal"),
          staticMethod().onClass("java.util.Objects").named("equals"));

  private static final Matcher<ExpressionTree> ACTUAL_METHOD =
      anyOf(
          instanceMethod()
              .onDescendantOf("com.google.common.truth.Subject")
              .named("actual")
              .withParameters(),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.Subject")
              .named("getSubject")
              .withParameters());
}
