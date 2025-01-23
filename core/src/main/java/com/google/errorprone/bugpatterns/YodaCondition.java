/*
 * Copyright 2023 The Error Prone Authors.
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
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getNullnessValue;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isStatic;
import static java.lang.String.format;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/** See the summary. */
@BugPattern(
    summary =
        "The non-constant portion of a comparison generally comes first. For equality, prefer"
            + " e.equals(CONSTANT) if e is non-null or Objects.equals(e, CONSTANT) if e may be"
            + " null. For standard operators, prefer e <OPERATION> CONSTANT.",
    severity = WARNING)
public final class YodaCondition extends BugChecker
    implements BinaryTreeMatcher, MethodInvocationTreeMatcher {
  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    return switch (tree.getKind()) {
      case EQUAL_TO, NOT_EQUAL_TO, LESS_THAN, GREATER_THAN, LESS_THAN_EQUAL, GREATER_THAN_EQUAL ->
          fix(
              tree,
              tree.getLeftOperand(),
              tree.getRightOperand(),
              /* provideNullSafeFix= */ false,
              state);
      default -> NO_MATCH;
    };
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (staticEqualsInvocation().matches(tree, state)) {
      return fix(
          tree,
          tree.getArguments().get(0),
          tree.getArguments().get(1),
          /* provideNullSafeFix= */ false,
          state);
    }
    if (instanceEqualsInvocation().matches(tree, state)) {
      ExpressionTree receiver = getReceiver(tree);
      if (receiver == null) {
        // call to equals implicitly qualified by `this`
        return NO_MATCH;
      }
      return fix(tree, receiver, tree.getArguments().get(0), /* provideNullSafeFix= */ true, state);
    }
    return NO_MATCH;
  }

  private Description fix(
      Tree tree,
      ExpressionTree lhs,
      ExpressionTree rhs,
      boolean provideNullSafeFix,
      VisitorState state) {
    if (!yodaCondition(lhs, rhs)) {
      return NO_MATCH;
    }
    if (isInequality(tree) && hasAdjacentComparison(state)) {
      return NO_MATCH;
    }

    var description = buildDescription(lhs);
    if (provideNullSafeFix
        && !getNullnessValue(rhs, state, NullnessAnalysis.instance(state.context))
            .equals(Nullness.NONNULL)) {
      var fix = SuggestedFix.builder().setShortDescription("null-safe fix");
      description.addFix(
          fix.replace(
                  tree,
                  format(
                      "%s.equals(%s, %s)",
                      qualifyType(state, fix, Objects.class.getName()),
                      state.getSourceForNode(rhs),
                      state.getSourceForNode(lhs)))
              .build());
    }
    return description
        .addFix(
            isInequality(tree)
                ? SuggestedFix.replace(
                    tree,
                    format(
                        "%s %s %s",
                        state.getSourceForNode(rhs), inverse(tree), state.getSourceForNode(lhs)))
                : SuggestedFix.swap(lhs, rhs))
        .build();
  }

  @SuppressWarnings("TreeToString") // Can't think of a better approach.
  private static boolean hasAdjacentComparison(VisitorState state) {
    BinaryTree tree = (BinaryTree) state.getPath().getLeaf();

    ConstantKind l = seemsConstant(tree.getLeftOperand());
    ConstantKind r = seemsConstant(tree.getRightOperand());
    boolean putativeVariableOnRight = l.constness > r.constness;
    if (putativeVariableOnRight) {
      ExpressionTree right = expressionToRight(state);
      return right != null && right.toString().equals(tree.getRightOperand().toString());
    }
    return false;
  }

  private static @Nullable ExpressionTree expressionToRight(VisitorState state) {
    TreePath path = state.getPath();
    while (true) {
      Tree tree = path.getLeaf();
      TreePath parentPath = path.getParentPath();
      Tree parent = parentPath.getLeaf();
      if (!(parent instanceof BinaryTree binaryTree)) {
        break;
      }
      if (binaryTree.getLeftOperand() == tree) {
        Tree right = binaryTree.getRightOperand();
        return isInequality(right) ? ((BinaryTree) right).getLeftOperand() : null;
      } else {
        path = path.getParentPath();
      }
    }
    return null;
  }

  private static boolean isInequality(Tree tree) {
    return switch (tree.getKind()) {
      case LESS_THAN, GREATER_THAN, LESS_THAN_EQUAL, GREATER_THAN_EQUAL -> true;
      default -> false;
    };
  }

  private static String inverse(Tree tree) {
    return switch (tree.getKind()) {
      case LESS_THAN -> ">";
      case GREATER_THAN -> "<";
      case LESS_THAN_EQUAL -> ">=";
      case GREATER_THAN_EQUAL -> "<=";
      default -> throw new AssertionError();
    };
  }

  private static boolean yodaCondition(ExpressionTree lhs, ExpressionTree rhs) {
    ConstantKind l = seemsConstant(lhs);
    ConstantKind r = seemsConstant(rhs);
    return l.constness > r.constness;
  }

  enum ConstantKind {
    NULL(3),
    CONSTANT(2),
    CONSTANT_VARIABLE(1),
    NON_CONSTANT(0);

    final int constness;

    ConstantKind(int constness) {
      this.constness = constness;
    }
  }

  private static ConstantKind seemsConstant(Tree tree) {
    if (constValue(tree) != null) {
      return ConstantKind.CONSTANT;
    }
    if (tree.getKind().equals(Tree.Kind.NULL_LITERAL)) {
      return ConstantKind.NULL;
    }
    var symbol = getSymbol(tree);
    if (symbol instanceof VarSymbol
        && (symbol.isEnum()
            || (isStatic(symbol)
                && CONSTANT_CASE.matcher(symbol.getSimpleName().toString()).matches()))) {
      return ConstantKind.CONSTANT_VARIABLE;
    }
    return ConstantKind.NON_CONSTANT;
  }

  private static final Pattern CONSTANT_CASE = Pattern.compile("[A-Z0-9_]+");
}
