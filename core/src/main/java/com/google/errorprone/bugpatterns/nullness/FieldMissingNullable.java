/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.bugpatterns.nullness.ReturnMissingNullable.hasDefinitelyNullBranch;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.sun.source.tree.Tree.Kind.EQUAL_TO;
import static com.sun.source.tree.Tree.Kind.NOT_EQUAL_TO;
import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;
import static javax.lang.model.element.ElementKind.FIELD;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import javax.annotation.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "FieldMissingNullable",
    summary =
        "Fields is assigned (or compared against) a definitely null value but is not annotated"
            + " @Nullable",
    severity = SUGGESTION)
public class FieldMissingNullable extends BugChecker
    implements BinaryTreeMatcher, AssignmentTreeMatcher, VariableTreeMatcher {
  /*
   * TODO(cpovirk): Consider providing a flag to make this checker still more conservative, similar
   * to what we provide in ReturnMissingNullable. In particular, consider skipping fields whose type
   * is a type-variable usage.
   */

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    if (tree.getKind() != EQUAL_TO && tree.getKind() != NOT_EQUAL_TO) {
      return NO_MATCH;
    }
    if (tree.getRightOperand().getKind() != NULL_LITERAL) {
      return NO_MATCH;
    }
    // TODO(cpovirk): Consider not adding @Nullable in cases like `checkState(foo != null)`.
    return matchIfLocallyDeclaredReferenceFieldWithoutNullable(
        getSymbol(tree.getLeftOperand()), tree, state);
  }

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    return match(getSymbol(tree.getVariable()), tree.getExpression(), state);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    return match(getSymbol(tree), tree.getInitializer(), state);
  }

  private Description match(Symbol assigned, ExpressionTree expression, VisitorState state) {
    if (expression == null) {
      return NO_MATCH;
    }

    if (!hasDefinitelyNullBranch(
        expression,
        // TODO(cpovirk): Precompute a set of definitelyNullVars instead of passing an empty set.
        ImmutableSet.of(),
        state)) {
      return NO_MATCH;
    }

    return matchIfLocallyDeclaredReferenceFieldWithoutNullable(assigned, expression, state);
  }

  private Description matchIfLocallyDeclaredReferenceFieldWithoutNullable(
      Symbol assigned, ExpressionTree treeToReportOn, VisitorState state) {
    if (assigned == null || assigned.getKind() != FIELD || assigned.type.isPrimitive()) {
      return NO_MATCH;
    }

    if (NullnessAnnotations.fromAnnotationsOn(assigned).orElse(null) == Nullness.NULLABLE) {
      return NO_MATCH;
    }

    VariableTree fieldDecl = findDeclaration(state, assigned);
    if (fieldDecl == null) {
      return NO_MATCH;
    }

    return describeMatch(treeToReportOn, NullnessFixes.makeFix(state, fieldDecl));
  }

  @Nullable
  private static VariableTree findDeclaration(VisitorState state, Symbol field) {
    JavacProcessingEnvironment javacEnv = JavacProcessingEnvironment.instance(state.context);
    TreePath fieldDeclPath = Trees.instance(javacEnv).getPath(field);
    // Skip fields declared in other compilation units since we can't make a fix for them here.
    if (fieldDeclPath != null
        && fieldDeclPath.getCompilationUnit() == state.getPath().getCompilationUnit()
        && (fieldDeclPath.getLeaf() instanceof VariableTree)) {
      return (VariableTree) fieldDeclPath.getLeaf();
    }
    return null;
  }
}
