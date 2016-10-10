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
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ArrayAccessTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.TrustingNullnessAnalysis;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;

/** @author kmb@google.com (Kevin Bierhoff) */
@BugPattern(
  name = "ParameterNotNullable",
  summary = "Method parameters that aren't checked for null shouldn't be annotated @Nullable",
  category = JDK,
  severity = SUGGESTION
)
public class ParameterNotNullable extends BugChecker
    implements MemberSelectTreeMatcher, ArrayAccessTreeMatcher {

  @Override
  public Description matchArrayAccess(ArrayAccessTree tree, VisitorState state) {
    return matchDereference(tree.getExpression(), state);
  }

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    return matchDereference(tree.getExpression(), state);
  }

  private Description matchDereference(ExpressionTree dereferencedExpression, VisitorState state) {
    Symbol dereferenced = ASTHelpers.getSymbol(dereferencedExpression);
    if (dereferenced == null
        || dereferenced.getKind() != ElementKind.PARAMETER
        || dereferenced.type.isPrimitive()) {
      return Description.NO_MATCH; // not a parameter dereference
    }

    if (!TrustingNullnessAnalysis.hasNullableAnnotation(dereferenced)) {
      return Description.NO_MATCH;
    }

    Nullness nullness =
        TrustingNullnessAnalysis.instance(state.context)
            .getNullness(new TreePath(state.getPath(), dereferencedExpression), state.context);
    if (nullness != Nullness.NULLABLE) {
      return Description.NO_MATCH;
    }

    for (AnnotationTree anno :
        findDeclaration(state, dereferenced).getModifiers().getAnnotations()) {
      if (ASTHelpers.getSymbol(anno).type.toString().endsWith(".Nullable")) {
        return buildDescription(dereferencedExpression)
            .setMessage("Nullable parameter not checked for null")
            .addFix(SuggestedFix.delete(anno))
            .build();
      }
    }
    // Shouldn't get here
    return Description.NO_MATCH;
  }

  @Nullable
  private VariableTree findDeclaration(VisitorState state, Symbol parameter) {
    JavacProcessingEnvironment javacEnv = JavacProcessingEnvironment.instance(state.context);
    TreePath declPath = Trees.instance(javacEnv).getPath(parameter);
    if (declPath != null
        && declPath.getCompilationUnit() == state.getPath().getCompilationUnit()
        && (declPath.getLeaf() instanceof VariableTree)) {
      return (VariableTree) declPath.getLeaf();
    }
    return null;
  }
}
