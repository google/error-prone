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
import static com.google.errorprone.matchers.UnusedReturnValueMatcher.isReturnValueUnused;
import static com.google.errorprone.matchers.field.FieldMatchers.staticField;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getNullnessValue;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isStatic;
import static com.google.errorprone.util.ASTHelpers.requiresParentheses;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;
import static java.lang.String.format;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import org.jspecify.annotations.Nullable;
import org.safere.Pattern;

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
    if (!yodaCondition(lhs, rhs, state)) {
      return NO_MATCH;
    }
    if (isInequality(tree) && hasAdjacentComparison(state)) {
      return NO_MATCH;
    }

    Type rhsType = getType(rhs);
    if (isSameType(rhsType, state.getSymtab().booleanType, state)) {
      Boolean lhsBool = booleanConstant(lhs, state);
      if (lhsBool != null
          && (isBinaryEquality(tree)
              || (tree instanceof MethodInvocationTree methodInvocation
                  && !isReturnValueUnused(methodInvocation, state)))) {
        boolean negate = lhsBool == tree.getKind().equals(Tree.Kind.NOT_EQUAL_TO);
        return simplifyBoolean(tree, lhs, rhs, negate, state);
      }
    }

    String lhsSource = lhsSource(tree, lhs, state);
    if (provideNullSafeFix && rhsType != null && rhsType.isPrimitive()) {
      // `x.equals(CONST)` doesn't compile for primitive `x`.
      return describeMatch(lhs, objectsEqualsFix(tree, rhs, lhsSource, state).build());
    }

    var description = buildDescription(lhs);
    if (provideNullSafeFix
        && !getNullnessValue(rhs, state, NullnessAnalysis.instance(state.context))
            .equals(Nullness.NONNULL)) {
      description.addFix(
          objectsEqualsFix(tree, rhs, lhsSource, state)
              .setShortDescription("null-safe fix")
              .build());
    }
    return description
        .addFix(
            isInequality(tree)
                ? SuggestedFix.replace(
                    tree, format("%s %s %s", state.getSourceForNode(rhs), inverse(tree), lhsSource))
                : SuggestedFix.builder()
                    .replace(lhs, state.getSourceForNode(rhs))
                    .replace(rhs, lhsSource)
                    .build())
        .build();
  }

  private Description simplifyBoolean(
      Tree tree, ExpressionTree lhs, ExpressionTree rhs, boolean negate, VisitorState state) {
    ExpressionTree expr = stripParentheses(rhs);
    if (negate
        && expr instanceof UnaryTree unary
        && unary.getKind().equals(Tree.Kind.LOGICAL_COMPLEMENT)) {
      expr = stripParentheses(unary.getExpression());
      negate = false;
    }
    String exprSource = state.getSourceForNode(expr);
    if (negate) {
      boolean parens = requiresParentheses(expr, state);
      return describeMatch(
          lhs, SuggestedFix.replace(tree, parens ? "!(" + exprSource + ")" : "!" + exprSource));
    }
    boolean parens =
        requiresParentheses(expr, state)
            && needsParentheses(state.getPath().getParentPath().getLeaf());
    return describeMatch(
        lhs, SuggestedFix.replace(tree, parens ? "(" + exprSource + ")" : exprSource));
  }

  private static SuggestedFix.Builder objectsEqualsFix(
      Tree tree, ExpressionTree rhs, String lhsSource, VisitorState state) {
    var fix = SuggestedFix.builder();
    return fix.replace(
        tree,
        format(
            "%s.equals(%s, %s)",
            qualifyType(state, fix, "java.util.Objects"), state.getSourceForNode(rhs), lhsSource));
  }

  private static boolean needsParentheses(Tree parent) {
    return switch (parent) {
      case UnaryTree unused -> true;
      case BinaryTree unused -> true;
      case ConditionalExpressionTree unused -> true;
      case TypeCastTree unused -> true;
      default -> false;
    };
  }

  private static String lhsSource(Tree tree, ExpressionTree lhs, VisitorState state) {
    if (!(tree instanceof BinaryTree)) {
      Boolean lhsBool = booleanConstant(lhs, state);
      if (lhsBool != null) {
        return lhsBool.toString();
      }
    }
    return state.getSourceForNode(lhs);
  }

  private static @Nullable Boolean booleanConstant(ExpressionTree tree, VisitorState state) {
    if (BOOLEAN_TRUE.matches(tree, state)) {
      return true;
    }
    if (BOOLEAN_FALSE.matches(tree, state)) {
      return false;
    }
    return null;
  }

  private static boolean isBinaryEquality(Tree tree) {
    return switch (tree.getKind()) {
      case EQUAL_TO, NOT_EQUAL_TO -> true;
      default -> false;
    };
  }

  @SuppressWarnings("TreeToString") // Can't think of a better approach.
  private static boolean hasAdjacentComparison(VisitorState state) {
    BinaryTree tree = (BinaryTree) state.getPath().getLeaf();

    ConstantKind l = seemsConstant(tree.getLeftOperand(), state);
    ConstantKind r = seemsConstant(tree.getRightOperand(), state);
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

  private static boolean yodaCondition(ExpressionTree lhs, ExpressionTree rhs, VisitorState state) {
    ConstantKind l = seemsConstant(lhs, state);
    ConstantKind r = seemsConstant(rhs, state);
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

  private static final Matcher<ExpressionTree> IGNORELIST =
      staticField().onClass("android.os.Build.VERSION").named("SDK_INT");

  private static final Matcher<ExpressionTree> BOOLEAN_TRUE =
      staticField().onClass("java.lang.Boolean").named("TRUE");

  private static final Matcher<ExpressionTree> BOOLEAN_FALSE =
      staticField().onClass("java.lang.Boolean").named("FALSE");

  private static ConstantKind seemsConstant(ExpressionTree tree, VisitorState state) {
    if (IGNORELIST.matches(tree, state)) {
      return ConstantKind.NON_CONSTANT;
    }
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
