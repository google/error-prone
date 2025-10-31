/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.sun.source.tree.Tree.Kind.LOGICAL_COMPLEMENT;
import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.TreeInfo;

/**
 * @author galitch@google.com (Anton Galitch)
 */
@BugPattern(
    summary =
        "Java assert is used in testing code. For testing purposes, prefer using Truth-based"
            + " assertions.",
    severity = SeverityLevel.ERROR)
public class UseCorrectAssertInTests extends BugChecker implements MethodTreeMatcher {
  private static final String STATIC_ASSERT_THAT_IMPORT =
      "static com.google.common.truth.Truth.assertThat";
  private static final String STATIC_ASSERT_WITH_MESSAGE_IMPORT =
      "static com.google.common.truth.Truth.assertWithMessage";

  private static final String IS_TRUE = "isTrue();";
  private static final String IS_FALSE = "isFalse();";

  private static final String IS_NULL = "isNull();";
  private static final String IS_NOT_NULL = "isNotNull();";

  private static final String IS_EQUAL_TO = "isEqualTo(%s);";
  private static final String IS_NOT_EQUAL_TO = "isNotEqualTo(%s);";

  private static final String IS_SAME_AS = "isSameInstanceAs(%s);";
  private static final String IS_NOT_SAME_AS = "isNotSameInstanceAs(%s);";

  public UseCorrectAssertInTests() {}

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    if (methodTree.getBody() == null) {
      // Method is not implemented (i.e. it's abstract).
      return Description.NO_MATCH;
    }

    // Match either JUnit tests or test-only code.
    if (!(ASTHelpers.isJUnitTestCode(state) || state.errorProneOptions().isTestOnlyTarget())) {
      return Description.NO_MATCH;
    }

    ImmutableList<AssertTree> assertions = scanAsserts(methodTree);
    if (assertions.isEmpty()) {
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder fix = SuggestedFix.builder();
    for (AssertTree foundAssert : assertions) {
      replaceAssert(fix, foundAssert, state);
    }
    // Emit finding on the first assertion rather than on the method (b/175575632).
    return describeMatch(assertions.get(0), fix.build());
  }

  private static void replaceAssert(
      SuggestedFix.Builder fix, AssertTree foundAssert, VisitorState state) {

    ExpressionTree expr = foundAssert.getCondition();
    expr = (ExpressionTree) TreeInfo.skipParens((JCTree) expr);

    // case: "assert !expr"
    if (expr.getKind().equals(LOGICAL_COMPLEMENT)) {
      addFix(fix, ((JCUnary) expr).getExpression(), foundAssert, state, IS_FALSE);
      return;
    }

    // case: "assert expr1.equals(expr2)"
    if (instanceMethod().anyClass().named("equals").matches(expr, state)) {
      JCMethodInvocation equalsCall = ((JCMethodInvocation) expr);
      JCExpression expr1 = ((JCFieldAccess) ((JCMethodInvocation) expr).meth).selected;
      JCExpression expr2 = equalsCall.getArguments().get(0);

      addFix(
          fix,
          expr1,
          foundAssert,
          state,
          String.format(IS_EQUAL_TO, normalizedSourceForExpression(expr2, state)));
      return;
    }

    // case: "assert expr1 == expr2" or "assert expr1 != expr2"
    if (expr.getKind().equals(Kind.EQUAL_TO) || expr.getKind().equals(Kind.NOT_EQUAL_TO)) {
      suggestFixForEquality(fix, foundAssert, state, expr.getKind().equals(Kind.EQUAL_TO));
      return;
    }

    // case: "assert expr", which didn't match any of the previous cases.
    addFix(fix, (JCExpression) expr, foundAssert, state, IS_TRUE);
  }

  private static void addFix(
      SuggestedFix.Builder fix,
      JCExpression expr,
      AssertTree foundAssert,
      VisitorState state,
      String isMethod) {

    String assertToUse;

    if (foundAssert.getDetail() == null) {
      fix.addImport(STATIC_ASSERT_THAT_IMPORT);
      assertToUse = String.format("assertThat(%s).", normalizedSourceForExpression(expr, state));
    } else {
      fix.addImport(STATIC_ASSERT_WITH_MESSAGE_IMPORT);
      assertToUse =
          String.format(
              "assertWithMessage(%s).that(%s).",
              convertToString(foundAssert.getDetail(), state),
              normalizedSourceForExpression(expr, state));
    }

    fix.replace(foundAssert, assertToUse + isMethod);
  }

  /** Handles the case "expr1 == expr2" */
  private static void suggestFixForEquality(
      SuggestedFix.Builder fix, AssertTree foundAssert, VisitorState state, boolean isEqual) {

    BinaryTree equalityTree = (BinaryTree) TreeInfo.skipParens((JCTree) foundAssert.getCondition());
    ExpressionTree expr1 = equalityTree.getLeftOperand();
    ExpressionTree expr2 = equalityTree.getRightOperand();

    if (expr1.getKind() == NULL_LITERAL) {
      // case: "assert null [op] expr"
      addFix(fix, (JCExpression) expr2, foundAssert, state, isEqual ? IS_NULL : IS_NOT_NULL);
    } else if (expr2.getKind() == NULL_LITERAL) {
      // case: "assert expr [op] null"
      addFix(fix, (JCExpression) expr1, foundAssert, state, isEqual ? IS_NULL : IS_NOT_NULL);
    } else if (ASTHelpers.getType(expr1).isPrimitive() || ASTHelpers.getType(expr2).isPrimitive()) {
      // case: eg. "assert expr == 1"
      addFix(
          fix,
          (JCExpression) expr1,
          foundAssert,
          state,
          String.format(isEqual ? IS_EQUAL_TO : IS_NOT_EQUAL_TO, state.getSourceForNode(expr2)));
    } else {
      // case: "assert expr1 [op] expr2"
      addFix(
          fix,
          (JCExpression) expr1,
          foundAssert,
          state,
          String.format(isEqual ? IS_SAME_AS : IS_NOT_SAME_AS, state.getSourceForNode(expr2)));
    }
  }

  private static String normalizedSourceForExpression(JCExpression expression, VisitorState state) {
    return state.getSourceForNode(TreeInfo.skipParens(expression));
  }

  /* Appends .toString() if the expression is not of type String. */
  private static String convertToString(ExpressionTree detail, VisitorState state) {
    return state.getSourceForNode(detail)
        + (ASTHelpers.isSameType(ASTHelpers.getType(detail), state.getSymtab().stringType, state)
            ? ""
            : ".toString()");
  }

  /** Returns all the "assert" expressions in the tree. */
  private static ImmutableList<AssertTree> scanAsserts(Tree tree) {
    ImmutableList.Builder<AssertTree> assertTrees = ImmutableList.builder();

    tree.accept(
        new TreeScanner<Void, VisitorState>() {
          @Override
          public Void visitAssert(AssertTree assertTree, VisitorState visitorState) {
            assertTrees.add(assertTree);
            return null;
          }
        },
        null);
    return assertTrees.build();
  }
}
