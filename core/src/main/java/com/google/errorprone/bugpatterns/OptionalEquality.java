/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NONNULL;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NULL;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "OptionalEquality",
  summary = "Comparison using reference equality instead of value equality",
  explanation =
      "Optionals should be compared for value equality using `.equals()`, and not for reference "
          + "equality using `==` and `!=`.",
  category = GUAVA,
  severity = ERROR,
  maturity = MATURE
)
public class OptionalEquality extends BugChecker implements BinaryTreeMatcher {

  private final NullnessAnalysis nullnessAnalysis = new NullnessAnalysis();

  @Override
  public final Description matchBinary(BinaryTree tree, VisitorState state) {
    switch (tree.getKind()) {
      case EQUAL_TO:
      case NOT_EQUAL_TO:
        break;
      default:
        return Description.NO_MATCH;
    }
    if (!isOptionalType(ASTHelpers.getType(tree.getLeftOperand()), state)) {
      return Description.NO_MATCH;
    }
    if (!isOptionalType(ASTHelpers.getType(tree.getRightOperand()), state)) {
      return Description.NO_MATCH;
    }

    Description.Builder builder = buildDescription(tree);
    addFixes(builder, tree, state);
    return builder.build();
  }

  void addFixes(Description.Builder builder, BinaryTree tree, VisitorState state) {
    String prefix = tree.getKind() == Kind.NOT_EQUAL_TO ? "!" : "";
    String lhs = state.getSourceForNode(tree.getLeftOperand());
    String rhs = state.getSourceForNode(tree.getRightOperand());
    Nullness nullness = getNullness(tree.getLeftOperand(), state);

    // If the lhs is possibly-null, provide both options.

    // Never swap the order (e.g. rhs.equals(lhs) when lhs is nullable and rhs is non-null), since
    // that has observable side-effects.

    if (nullness != NONNULL) {
      builder.addFix(
          SuggestedFix.builder()
              .replace(tree, String.format("%sObjects.equals(%s, %s)", prefix, lhs, rhs))
              .addImport("java.util.Objects")
              .build());
    }
    if (nullness != NULL) {
      builder.addFix(
          SuggestedFix.replace(tree, String.format("%s%s.equals(%s)", prefix, lhs, rhs)));
    }
  }

  private Nullness getNullness(ExpressionTree expr, VisitorState state) {
    TreePath pathToExpr = new TreePath(state.getPath(), expr);
    return nullnessAnalysis.getNullness(pathToExpr, state.context);
  }

  private static boolean isOptionalType(Type type, VisitorState state) {
    Type guavaOptional = state.getTypeFromString(com.google.common.base.Optional.class.getName());
    Type utilOptional = state.getTypeFromString("java.util.Optional");
    // TODO(cushon): write a test for the util.Optional case once we can depend on 8.
    return ASTHelpers.isSameType(type, guavaOptional, state)
        || ASTHelpers.isSameType(type, utilOptional, state);
  }
}
