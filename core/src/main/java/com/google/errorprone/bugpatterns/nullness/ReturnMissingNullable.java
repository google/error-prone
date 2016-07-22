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
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.TrustingNullnessAnalysis;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import javax.annotation.Nullable;

/** @author kmb@google.com (Kevin Bierhoff) */
@BugPattern(
  name = "ReturnMissingNullable",
  summary = "Methods that can return null should be annotated @Nullable",
  category = JDK,
  severity = SUGGESTION
)
public class ReturnMissingNullable extends BugChecker implements ReturnTreeMatcher {

  @Override
  public Description matchReturn(ReturnTree tree, VisitorState state) {
    ExpressionTree returnExpression = tree.getExpression();
    if (returnExpression == null) {
      return Description.NO_MATCH;
    }

    // Best-effort try to avoid running the dataflow analysis
    // TODO(kmb): bail on more non-null expressions, such as "this", arithmethic, logical, and &&/||
    if (ASTHelpers.constValue(returnExpression) != null) {
      // This should include literals such as "true" or a string
      return Description.NO_MATCH;
    }

    JCMethodDecl method = findSurroundingMethod(state.getPath());
    if (method == null || isIgnoredReturnType(method, state)) {
      return Description.NO_MATCH;
    }
    if (TrustingNullnessAnalysis.hasNullableAnnotation(method.sym)) {
      return Description.NO_MATCH;
    }

    // Don't need dataflow to tell us that null is nullable
    if (returnExpression.getKind() == ExpressionTree.Kind.NULL_LITERAL) {
      return makeFix(method, tree, "Returning null literal");
    }

    // OK let's see what dataflow says
    Nullness nullness =
        TrustingNullnessAnalysis.instance(state.context)
            .getNullness(new TreePath(state.getPath(), returnExpression), state.context);
    switch (nullness) {
      case BOTTOM:
      case NONNULL:
        return Description.NO_MATCH;
      case NULL:
        return makeFix(method, tree, "Definitely returning null");
      case NULLABLE:
        return makeFix(method, tree, "May return null");
      default:
        throw new AssertionError("Impossible: " + nullness);
    }
  }

  private boolean isIgnoredReturnType(JCMethodDecl method, VisitorState state) {
    Type returnType = method.sym.getReturnType();
    // Methods returning a primitive cannot return null.  Also ignore Void-returning methods as
    // the only valid Void value is null, so it's implied.
    // TODO(kmb): Technically we should assume NULL when we see a call to a method that returns Void
    return returnType.isPrimitiveOrVoid()
        || state.getTypes().isSameType(returnType, state.getTypeFromString("java.lang.Void"));
  }

  private Description makeFix(JCMethodDecl method, ReturnTree tree, String message) {
    return buildDescription(tree)
        .setMessage(message)
        .addFix(
            SuggestedFix.builder()
                .addImport("javax.annotation.Nullable")
                .prefixWith(method, "@Nullable\n")
                .build())
        .build();
  }

  @Nullable
  private static JCMethodDecl findSurroundingMethod(TreePath path) {
    while (path.getLeaf().getKind() != Kind.METHOD) {
      if (path.getLeaf().getKind() == Kind.LAMBDA_EXPRESSION) {
        // Ignore return statements in lambda expressions. There's no method declaration to suggest
        // annotations for anyway.
        return null;
      }
      path = path.getParentPath();
    }
    return (JCMethodDecl) path.getLeaf();
  }
}
