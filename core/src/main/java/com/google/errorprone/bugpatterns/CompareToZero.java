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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFixes.renameMethodInvocation;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.ProtobufMatchers.PROTO_DURATIONS_UTIL;
import static com.google.errorprone.matchers.ProtobufMatchers.PROTO_TIMESTAMPS_UTIL;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ClassName;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import java.util.function.Function;
import javax.inject.Inject;

/** Suggests comparing the result of {@code compareTo} to only {@code 0}. */
@BugPattern(
    summary =
        """
        The result of #compareTo or #compare should only be compared to 0. It is an \
        implementation detail whether a given type returns strictly the values {-1, 0, +1} \
        or others.\
        """,
    severity = ERROR)
public final class CompareToZero extends BugChecker implements MethodInvocationTreeMatcher {
  private static final String SUGGEST_IMPROVEMENT =
      """
      It is generally more robust (and readable) to compare the result of #compareTo/#compare to \
      0. Although the suggested replacement is identical in this case, we'd suggest it for \
      consistency.\
      """;

  private static final String NON_RELATIONAL_OPERATOR =
      """
      It is unsafe to perform arithmetic or logical operations on the result of #compareTo or \
      #compare, as the return value is not guaranteed to be strictly in the range {-1, 0, \
      +1}. Consider using Integer.signum to normalize the result before performing \
      operations.\
      """;

  private static final String ARITHMETIC_OPERATOR =
      """
      It is unsafe to perform arithmetic operations on the result of #compareTo or #compare, as \
      the return value is not guaranteed to be strictly in the range {-1, 0, +1}, and operations \
      (especially multiplication) can also overflow. Consider using Integer.signum on the result \
      of #compareTo or #compare.\
      """;

  private static final String NEGATION_OPERATOR =
      """
      It is unsafe to negate the result of #compareTo or #compare, as negating the result can \
      overflow if compareTo returns Integer.MIN_VALUE. Consider using Integer.signum on \
      the result of #compareTo or #compare.\
      """;

  private static final ImmutableSet<Kind> COMPARISONS =
      ImmutableSet.of(
          Kind.EQUAL_TO,
          Kind.NOT_EQUAL_TO,
          Kind.LESS_THAN,
          Kind.LESS_THAN_EQUAL,
          Kind.GREATER_THAN,
          Kind.GREATER_THAN_EQUAL);

  private static final ImmutableMap<Kind, Kind> REVERSE =
      ImmutableMap.of(
          Kind.LESS_THAN, Kind.GREATER_THAN,
          Kind.LESS_THAN_EQUAL, Kind.GREATER_THAN_EQUAL,
          Kind.GREATER_THAN, Kind.LESS_THAN,
          Kind.GREATER_THAN_EQUAL, Kind.LESS_THAN_EQUAL);

  private static final ImmutableSet<Kind> ARITHMETIC_OPERATORS =
      ImmutableSet.of(Kind.PLUS, Kind.MINUS, Kind.MULTIPLY, Kind.DIVIDE);

  /** The Truth assertions that compare the subject to their argument, and their operators. */
  private static final ImmutableMap<String, Kind> TRUTH_COMPARISONS =
      ImmutableMap.of(
          "isEqualTo", Kind.EQUAL_TO,
          "isNotEqualTo", Kind.NOT_EQUAL_TO,
          "isLessThan", Kind.LESS_THAN,
          "isAtMost", Kind.LESS_THAN_EQUAL,
          "isGreaterThan", Kind.GREATER_THAN,
          "isAtLeast", Kind.GREATER_THAN_EQUAL);

  /** The Truth assertions for the operators that fixes compare to {@code 0} with. */
  private static final ImmutableMap<String, String> TRUTH_ASSERTIONS =
      ImmutableMap.of("<", "isLessThan", "<=", "isAtMost", ">", "isGreaterThan", ">=", "isAtLeast");

  private static final Matcher<ExpressionTree> COMPARE_TO =
      anyOf(
          staticMethod()
              .onClassAny(
                  ClassName.of("com.google.common.primitives.Booleans"),
                  ClassName.of("com.google.common.primitives.Chars"),
                  ClassName.of("com.google.common.primitives.Doubles"),
                  ClassName.of("com.google.common.primitives.Floats"),
                  ClassName.of("com.google.common.primitives.Ints"),
                  ClassName.of("com.google.common.primitives.Longs"),
                  ClassName.of("com.google.common.primitives.Shorts"),
                  ClassName.of("com.google.common.primitives.SignedBytes"),
                  ClassName.of("com.google.common.primitives.UnsignedBytes"),
                  ClassName.of("com.google.common.primitives.UnsignedInts"),
                  ClassName.of("com.google.common.primitives.UnsignedLongs"),
                  PROTO_DURATIONS_UTIL,
                  PROTO_TIMESTAMPS_UTIL,
                  ClassName.of("java.lang.Boolean"),
                  ClassName.of("java.lang.Byte"),
                  ClassName.of("java.lang.Character"),
                  ClassName.of("java.lang.Double"),
                  ClassName.of("java.lang.Float"),
                  ClassName.of("java.lang.Integer"),
                  ClassName.of("java.lang.Long"),
                  ClassName.of("java.lang.Short"),
                  ClassName.of("java.util.Objects"))
              .named("compare"),
          instanceMethod().onDescendantOf("java.lang.Comparable").named("compareTo"),
          instanceMethod().onDescendantOf("java.util.Comparator").named("compare"));

  private static final Matcher<ExpressionTree> ASSERT_THAT =
      anyOf(
          staticMethod().onClass("com.google.common.truth.Truth").named("assertThat"),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
              .named("that"));

  private static final Matcher<ExpressionTree> TRUTH_COMPARISON =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.ComparableSubject")
          .namedAnyOf(TRUTH_COMPARISONS.keySet());

  private final boolean matchTruthAssertions;

  @Inject
  CompareToZero(ErrorProneFlags flags) {
    this.matchTruthAssertions = flags.getBoolean("CompareToZero:MatchTruthAssertions").orElse(true);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (COMPARE_TO.matches(tree, state)) {
      new Visitor().visitParent(state);
    }
    return NO_MATCH;
  }

  private final class Visitor extends SimpleTreeVisitor<Void, VisitorState> {
    private Tree child;

    @Override
    public Void visitParenthesized(ParenthesizedTree parenthesizedTree, VisitorState state) {
      return visitParent(state);
    }

    @Override
    public Void visitUnary(UnaryTree unaryTree, VisitorState state) {
      if (unaryTree.getKind() == Kind.UNARY_MINUS) {
        state.reportMatch(buildDescription(unaryTree).setMessage(NEGATION_OPERATOR).build());
      } else if (unaryTree.getKind() == Kind.BITWISE_COMPLEMENT) {
        state.reportMatch(buildDescription(unaryTree).setMessage(NON_RELATIONAL_OPERATOR).build());
      }
      return null;
    }

    @Override
    public Void visitSwitch(SwitchTree switchTree, VisitorState state) {
      return handleSwitch(switchTree, switchTree.getExpression(), state);
    }

    @Override
    public Void visitSwitchExpression(
        SwitchExpressionTree switchExpressionTree, VisitorState state) {
      return handleSwitch(switchExpressionTree, switchExpressionTree.getExpression(), state);
    }

    private Void handleSwitch(Tree switchTree, ExpressionTree expression, VisitorState state) {
      if (expression == child) {
        ExpressionTree selector = stripParentheses(expression);
        SuggestedFix fix =
            SuggestedFix.builder()
                .addStaticImport("java.lang.Integer.signum")
                .replace(selector, String.format("signum(%s)", state.getSourceForNode(selector)))
                .build();
        state.reportMatch(describeMatch(switchTree, fix));
      }
      return null;
    }

    @Override
    public Void visitBinary(BinaryTree binaryTree, VisitorState state) {
      Kind kind = binaryTree.getKind();

      // `child` is the Tree we had before bubbling up to this BinaryTree. Check which side it
      // corresponds to.
      boolean reversed = binaryTree.getRightOperand() == child;
      ExpressionTree comparatorSide =
          reversed ? binaryTree.getRightOperand() : binaryTree.getLeftOperand();
      ExpressionTree otherSide =
          reversed ? binaryTree.getLeftOperand() : binaryTree.getRightOperand();

      if (!COMPARISONS.contains(kind)) {
        boolean isStringConcat =
            isSameType(getType(binaryTree), state.getSymtab().stringType, state);
        String message =
            ARITHMETIC_OPERATORS.contains(kind) && !isStringConcat
                ? ARITHMETIC_OPERATOR
                : NON_RELATIONAL_OPERATOR;
        state.reportMatch(buildDescription(binaryTree).setMessage(message).build());
        return null;
      }

      Integer constantInt = constValue(otherSide, Integer.class);
      if (constantInt == null) {
        return null;
      }
      if (constantInt == 0) {
        return null;
      }
      if (reversed && kind != Kind.EQUAL_TO) {
        kind = REVERSE.get(kind);
      }
      if (kind == null) {
        return null;
      }
      reportComparison(
          binaryTree,
          kind,
          constantInt,
          comparator -> generateFix(binaryTree, state, comparatorSide, comparator),
          state);
      return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      if (!matchTruthAssertions
          || tree.getArguments().size() != 1
          || tree.getArguments().getFirst() != child
          || !isSameType(getType(child), state.getSymtab().intType, state)
          || !ASSERT_THAT.matches(tree, state)) {
        return null;
      }
      TreePath parentPath = state.getPath().getParentPath();
      while (parentPath.getLeaf() instanceof ParenthesizedTree) {
        parentPath = parentPath.getParentPath();
      }
      if (!(parentPath.getLeaf() instanceof MemberSelectTree select)
          || !(parentPath.getParentPath().getLeaf() instanceof MethodInvocationTree assertion)
          || assertion.getMethodSelect() != select
          || !TRUTH_COMPARISON.matches(assertion, state)
          || assertion.getArguments().size() != 1) {
        return null;
      }
      ExpressionTree expected = getOnlyElement(assertion.getArguments());
      Integer constantInt = constValue(expected, Integer.class);
      if (constantInt == null || constantInt == 0) {
        return null;
      }
      reportComparison(
          assertion,
          TRUTH_COMPARISONS.get(getSymbol(assertion).getSimpleName().toString()),
          constantInt,
          comparator ->
              SuggestedFix.builder()
                  .merge(renameMethodInvocation(assertion, TRUTH_ASSERTIONS.get(comparator), state))
                  .replace(expected, "0")
                  .build(),
          state);
      return null;
    }

    /**
     * Reports a comparison of the result of {@code compareTo} or {@code compare} to a constant
     * other than {@code 0}.
     *
     * @param tree the comparison to report
     * @param kind the operator of the comparison, with the result as its left operand
     * @param constantInt the constant that the result is compared to
     * @param fixer returns a fix that compares the result to {@code 0} with the given operator
     */
    private void reportComparison(
        ExpressionTree tree,
        Kind kind,
        int constantInt,
        Function<String, SuggestedFix> fixer,
        VisitorState state) {
      if (kind == Kind.EQUAL_TO) {
        SuggestedFix fix = fixer.apply(constantInt < 0 ? "<" : ">");
        state.reportMatch(describeMatch(tree, fix));
        return;
      }
      if ((kind == Kind.GREATER_THAN || kind == Kind.NOT_EQUAL_TO) && constantInt == -1) {
        SuggestedFix fix = fixer.apply(">=");
        state.reportMatch(describeMatch(tree, fix));
        return;
      }
      if ((kind == Kind.LESS_THAN || kind == Kind.NOT_EQUAL_TO) && constantInt == 1) {
        SuggestedFix fix = fixer.apply("<=");
        state.reportMatch(describeMatch(tree, fix));
        return;
      }
      if (kind == Kind.LESS_THAN_EQUAL && constantInt == -1) {
        SuggestedFix fix = fixer.apply("<");
        state.reportMatch(
            buildDescription(tree).setMessage(SUGGEST_IMPROVEMENT).addFix(fix).build());
        return;
      }
      if (kind == Kind.GREATER_THAN_EQUAL && constantInt == 1) {
        SuggestedFix fix = fixer.apply(">");
        state.reportMatch(
            buildDescription(tree).setMessage(SUGGEST_IMPROVEMENT).addFix(fix).build());
        return;
      }
      state.reportMatch(describeMatch(tree));
    }

    private SuggestedFix generateFix(
        BinaryTree binaryTree,
        VisitorState state,
        ExpressionTree comparatorSide,
        String comparator) {
      return SuggestedFix.replace(
          binaryTree, String.format("%s %s 0", state.getSourceForNode(comparatorSide), comparator));
    }

    private Void visitParent(VisitorState state) {
      child = state.getPath().getLeaf();
      TreePath parentPath = state.getPath().getParentPath();
      return visit(parentPath.getLeaf(), state.withPath(parentPath));
    }
  }
}
