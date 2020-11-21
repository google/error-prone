/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NONNULL;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NULL;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.List;
import java.util.Optional;

/**
 * Abstract implementation of a BugPattern that detects the use of reference equality to compare
 * classes with value semantics.
 *
 * <p>See e.g. {@link NumericEquality}, {@link OptionalEquality}, {@link
 * ProtoStringFieldReferenceEquality}, and {@link StringEquality}.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public abstract class AbstractReferenceEquality extends BugChecker implements BinaryTreeMatcher {

  private static final Matcher<MethodInvocationTree> EQUALS_STATIC_METHODS =
      staticEqualsInvocation();

  private static final Matcher<ExpressionTree> OBJECT_INSTANCE_EQUALS = instanceEqualsInvocation();

  protected abstract boolean matchArgument(ExpressionTree tree, VisitorState state);

  @Override
  public final Description matchBinary(BinaryTree tree, VisitorState state) {
    switch (tree.getKind()) {
      case EQUAL_TO:
      case NOT_EQUAL_TO:
        break;
      default:
        return Description.NO_MATCH;
    }
    if (tree.getLeftOperand().getKind() == Kind.NULL_LITERAL
        || !matchArgument(tree.getLeftOperand(), state)) {
      return Description.NO_MATCH;
    }
    if (tree.getRightOperand().getKind() == Kind.NULL_LITERAL
        || !matchArgument(tree.getRightOperand(), state)) {
      return Description.NO_MATCH;
    }

    Description.Builder builder = buildDescription(tree);
    addFixes(builder, tree, state);
    return builder.build();
  }

  protected void addFixes(Description.Builder builder, BinaryTree tree, VisitorState state) {
    ExpressionTree lhs = tree.getLeftOperand();
    ExpressionTree rhs = tree.getRightOperand();

    Optional<Fix> fixToReplaceOrStatement = inOrStatementWithEqualsCheck(state, tree);
    if (fixToReplaceOrStatement.isPresent()) {
      builder.addFix(fixToReplaceOrStatement.get());
      return;
    }

    // Swap the order (e.g. rhs.equals(lhs) if the rhs is a non-null constant, and the lhs is not
    if (ASTHelpers.constValue(lhs) == null && ASTHelpers.constValue(rhs) != null) {
      ExpressionTree tmp = lhs;
      lhs = rhs;
      rhs = tmp;
    }

    String prefix = tree.getKind() == Kind.NOT_EQUAL_TO ? "!" : "";
    String lhsSource = state.getSourceForNode(lhs);
    String rhsSource = state.getSourceForNode(rhs);

    Nullness nullness = getNullness(lhs, state);

    // If the lhs is possibly-null, provide both options.
    if (nullness != NONNULL) {
      if (state.isAndroidCompatible()) {
        builder.addFix(
            SuggestedFix.builder()
                .replace(
                    tree, String.format("%sObjects.equal(%s, %s)", prefix, lhsSource, rhsSource))
                .addImport("com.google.common.base.Objects")
                .build());
      } else {
        builder.addFix(
            SuggestedFix.builder()
                .replace(
                    tree, String.format("%sObjects.equals(%s, %s)", prefix, lhsSource, rhsSource))
                .addImport("java.util.Objects")
                .build());
      }
    }
    if (nullness != NULL) {
      builder.addFix(
          SuggestedFix.replace(
              tree,
              String.format(
                  "%s%s.equals(%s)",
                  prefix,
                  lhs instanceof BinaryTree ? String.format("(%s)", lhsSource) : lhsSource,
                  rhsSource)));
    }
  }

  private static Optional<Fix> inOrStatementWithEqualsCheck(VisitorState state, BinaryTree tree) {
    // Only attempt to handle a == b || a.equals(b);
    if (tree.getKind() == Kind.NOT_EQUAL_TO) {
      return Optional.empty();
    }

    ExpressionTree lhs = tree.getLeftOperand();
    ExpressionTree rhs = tree.getRightOperand();

    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent.getKind() != Kind.CONDITIONAL_OR) {
      return Optional.empty();
    }
    BinaryTree p = (BinaryTree) parent;

    if (p.getLeftOperand() != tree) {
      // a == b is on the RHS, ignore this construction
      return Optional.empty();
    }

    // If the other half of this or statement is foo.equals(bar) or Objects.equals(foo, bar)
    // replace the or statement with the other half as already written.
    ExpressionTree otherExpression = ASTHelpers.stripParentheses(p.getRightOperand());
    if (!(otherExpression instanceof MethodInvocationTree)) {
      return Optional.empty();
    }
    MethodInvocationTree other = (MethodInvocationTree) otherExpression;

    // a == b || Objects.equals(a, b) => Objects.equals(a, b)
    if (EQUALS_STATIC_METHODS.matches(other, state)) {
      List<? extends ExpressionTree> arguments = other.getArguments();
      if (treesMatch(arguments.get(0), arguments.get(1), lhs, rhs)) {
        return Optional.of(SuggestedFix.replace(parent, state.getSourceForNode(otherExpression)));
      }
    }

    // a == b || a.equals(b) => a.equals(b)
    if (OBJECT_INSTANCE_EQUALS.matches(otherExpression, state)) {
      if (treesMatch(ASTHelpers.getReceiver(other), other.getArguments().get(0), lhs, rhs)) {
        return Optional.of(SuggestedFix.replace(parent, state.getSourceForNode(otherExpression)));
      }
    }
    return Optional.empty();
  }

  private Nullness getNullness(ExpressionTree expr, VisitorState state) {
    TreePath pathToExpr = new TreePath(state.getPath(), expr);
    return state.getNullnessAnalysis().getNullness(pathToExpr, state.context);
  }

  private static boolean treesMatch(
      ExpressionTree lhs1, ExpressionTree rhs1, ExpressionTree lhs2, ExpressionTree rhs2) {
    return (ASTHelpers.sameVariable(lhs1, lhs2) && ASTHelpers.sameVariable(rhs1, rhs2))
        || (ASTHelpers.sameVariable(lhs1, rhs2) && ASTHelpers.sameVariable(rhs1, lhs2));
  }
}
