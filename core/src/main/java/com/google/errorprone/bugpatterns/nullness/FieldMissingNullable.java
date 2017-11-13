/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.TrustingNullnessAnalysis;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;

/**
 * {@link Nullable} suggestions for fields based on values assigned to them. For simplicity this
 * check will not suggest annotations for fields that are never assigned in a constructor. While
 * fields like that <i>seem</i> like obvious candidates for being nullable, they are really not
 * because fields may be assigned to in methods called from a constructor or super-constructor, for
 * instance. We'd also need an analysis that tells us about uninitialized fields.
 *
 * @author kmb@google.com (Kevin Bierhoff)
 */
@BugPattern(
  name = "FieldMissingNullable",
  summary = "Fields that can be null should be annotated @Nullable",
  category = JDK,
  severity = SUGGESTION,
  providesFix = REQUIRES_HUMAN_ATTENTION
)
public class FieldMissingNullable extends BugChecker
    implements AssignmentTreeMatcher, VariableTreeMatcher {
  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    Symbol assigned = ASTHelpers.getSymbol(tree);
    if (assigned == null
        || assigned.getKind() != ElementKind.FIELD
        || assigned.type.isPrimitive()) {
      return Description.NO_MATCH; // not a field of nullable type
    }

    ExpressionTree expression = tree.getInitializer();
    if (expression == null || ASTHelpers.constValue(expression) != null) {
      // This should include literals such as "true" or a string
      return Description.NO_MATCH;
    }

    if (TrustingNullnessAnalysis.hasNullableAnnotation(assigned)) {
      return Description.NO_MATCH; // field already annotated
    }

    // Don't need dataflow to tell us that null is nullable
    if (expression.getKind() == Tree.Kind.NULL_LITERAL) {
      return makeFix(state, tree, tree, "Initializing field with null literal");
    }

    // OK let's see what dataflow says
    // TODO(kmb): Merge this method with matchAssignment once we unify nullness analysis entry point
    Nullness nullness =
        TrustingNullnessAnalysis.instance(state.context)
            .getFieldInitializerNullness(state.getPath(), state.context);
    switch (nullness) {
      case BOTTOM:
      case NONNULL:
        return Description.NO_MATCH;
      case NULL:
        return makeFix(state, tree, tree, "Initializing field with null");
      case NULLABLE:
        return makeFix(state, tree, tree, "May initialize field with null");
      default:
        throw new AssertionError("Impossible: " + nullness);
    }
  }

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    Symbol assigned = ASTHelpers.getSymbol(tree.getVariable());
    if (assigned == null
        || assigned.getKind() != ElementKind.FIELD
        || assigned.type.isPrimitive()) {
      return Description.NO_MATCH; // not a field of nullable type
    }

    // Best-effort try to avoid running the dataflow analysis
    // TODO(kmb): bail on more non-null expressions, such as "this", arithmethic, logical, and &&/||
    ExpressionTree expression = tree.getExpression();
    if (ASTHelpers.constValue(expression) != null) {
      // This should include literals such as "true" or a string
      return Description.NO_MATCH;
    }

    if (TrustingNullnessAnalysis.hasNullableAnnotation(assigned)) {
      return Description.NO_MATCH; // field already annotated
    }

    VariableTree fieldDecl = findDeclaration(state, assigned);
    if (fieldDecl == null) {
      return Description.NO_MATCH; // skip fields declared elsewhere for simplicity
    }

    // Don't need dataflow to tell us that null is nullable
    if (expression.getKind() == Tree.Kind.NULL_LITERAL) {
      return makeFix(state, fieldDecl, tree, "Assigning null literal to field");
    }

    // OK let's see what dataflow says
    Nullness nullness =
        TrustingNullnessAnalysis.instance(state.context)
            .getNullness(new TreePath(state.getPath(), expression), state.context);
    if (nullness == null) {
      // This can currently happen if the assignment is inside a finally block after a return.
      // TODO(b/69154806): Make dataflow work for that case.
      return Description.NO_MATCH;
    }
    switch (nullness) {
      case BOTTOM:
      case NONNULL:
        return Description.NO_MATCH;
      case NULL:
        return makeFix(state, fieldDecl, tree, "Assigning null to field");
      case NULLABLE:
        return makeFix(state, fieldDecl, tree, "May assign null to field");
      default:
        throw new AssertionError("Impossible: " + nullness);
    }
  }

  @Nullable
  private VariableTree findDeclaration(VisitorState state, Symbol field) {
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

  private Description makeFix(
      VisitorState state, VariableTree declaration, Tree matchedTree, String message) {
    return buildDescription(matchedTree)
        .setMessage(message)
        .addFix(NullnessFixes.makeFix(state, declaration))
        .build();
  }
}
