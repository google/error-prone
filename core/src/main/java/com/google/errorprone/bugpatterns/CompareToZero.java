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

import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.constValue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;

/** Suggests comparing the result of {@code compareTo} to only {@code 0}. */
@BugPattern(
    name = "CompareToZero",
    summary =
        "The result of #compareTo or #compare should only be compared to 0. It is an "
            + "implementation detail whether a given type returns strictly the values {-1, 0, +1} "
            + "or others.",
    providesFix = REQUIRES_HUMAN_ATTENTION,
    severity = WARNING)
public final class CompareToZero extends BugChecker implements MethodInvocationTreeMatcher {
  private static final String SUGGEST_IMPROVEMENT =
      "It is generally more robust (and readable) to compare the result of #compareTo/#compare to"
          + "0. Although the suggested replacement is identical in this case, we'd suggest it for"
          + "consistency.";

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

  private static final ImmutableSet<Kind> OTHER_STRANGE_OPERATIONS =
      ImmutableSet.of(Kind.PLUS, Kind.MINUS);

  private static final Matcher<ExpressionTree> COMPARE_TO =
      anyOf(
          instanceMethod().onDescendantOf("java.lang.Comparable").named("compareTo"),
          instanceMethod().onDescendantOf("java.util.Comparator").named("compare"));

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
    public Void visitBinary(BinaryTree binaryTree, VisitorState state) {
      Kind kind = binaryTree.getKind();
      if (OTHER_STRANGE_OPERATIONS.contains(kind)) {
        state.reportMatch(describeMatch(binaryTree));
        return null;
      }
      // `child` is the Tree we had before bubbling up to this BinaryTree. Check which side it
      // corresponds to.
      boolean reversed = binaryTree.getRightOperand() == child;
      ExpressionTree comparatorSide =
          reversed ? binaryTree.getRightOperand() : binaryTree.getLeftOperand();
      ExpressionTree otherSide =
          reversed ? binaryTree.getLeftOperand() : binaryTree.getRightOperand();
      Integer constantInt = constValue(otherSide, Integer.class);
      if (constantInt == null) {
        return null;
      }
      if (constantInt == 0) {
        return null;
      }
      if (binaryTree.getKind() == Kind.EQUAL_TO) {
        SuggestedFix fix =
            generateFix(binaryTree, state, comparatorSide, constantInt < 0 ? "<" : ">");
        state.reportMatch(describeMatch(binaryTree, fix));
        return null;
      }
      if (reversed) {
        kind = REVERSE.get(kind);
      }
      if (kind == null) {
        return null;
      }
      if ((kind == Kind.GREATER_THAN || kind == Kind.NOT_EQUAL_TO) && constantInt == -1) {
        SuggestedFix fix = generateFix(binaryTree, state, comparatorSide, ">=");
        state.reportMatch(describeMatch(binaryTree, fix));
        return null;
      }
      if ((kind == Kind.LESS_THAN || kind == Kind.NOT_EQUAL_TO) && constantInt == 1) {
        SuggestedFix fix = generateFix(binaryTree, state, comparatorSide, "<=");
        state.reportMatch(describeMatch(binaryTree, fix));
        return null;
      }
      if (kind == Kind.LESS_THAN_EQUAL && constantInt == -1) {
        SuggestedFix fix = generateFix(binaryTree, state, comparatorSide, "<");
        state.reportMatch(
            buildDescription(binaryTree).setMessage(SUGGEST_IMPROVEMENT).addFix(fix).build());
        return null;
      }
      if (kind == Kind.GREATER_THAN_EQUAL && constantInt == 1) {
        SuggestedFix fix = generateFix(binaryTree, state, comparatorSide, ">");
        state.reportMatch(
            buildDescription(binaryTree).setMessage(SUGGEST_IMPROVEMENT).addFix(fix).build());
      }
      if (COMPARISONS.contains(binaryTree.getKind())) {
        state.reportMatch(describeMatch(binaryTree));
      }
      return null;
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
