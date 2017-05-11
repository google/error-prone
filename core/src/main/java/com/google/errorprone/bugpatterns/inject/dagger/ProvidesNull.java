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

package com.google.errorprone.bugpatterns.inject.dagger;

import static com.google.errorprone.BugPattern.Category.DAGGER;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/**
 * Bug checker for null-returning methods annotated with {@code @Provides} but not
 * {@code @Nullable}.
 */
@BugPattern(
  name = "DaggerProvidesNull",
  summary = "Dagger @Provides methods may not return null unless annotated with @Nullable",
  explanation =
      "Dagger `@Provides` methods may not return null unless annotated with `@Nullable`. "
          + "Such a method will cause a `NullPointerException` at runtime if the `return null` "
          + "path is ever taken.\n\n"
          + "If you believe the `return null` path can never be taken, please throw a "
          + "`RuntimeException` instead. Otherwise, please annotate the method with `@Nullable`.",
  category = DAGGER,
  severity = ERROR
)
public class ProvidesNull extends BugChecker implements ReturnTreeMatcher {

  /**
   * Matches explicit "return null" statements in methods annotated with {@code @Provides} but not
   * {@code @Nullable}. Suggests either annotating the method with {@code @Nullable} or throwing a
   * {@link RuntimeException} instead.
   */
  // TODO(eaftan): Use nullness dataflow analysis when it's ready
  @Override
  public Description matchReturn(ReturnTree returnTree, VisitorState state) {
    ExpressionTree returnExpression = returnTree.getExpression();
    if (returnExpression == null || returnExpression.getKind() != Kind.NULL_LITERAL) {
      return Description.NO_MATCH;
    }

    TreePath path = state.getPath();
    MethodTree enclosingMethod = null;
    while (true) {
      if (path == null || path.getLeaf() instanceof LambdaExpressionTree) {
        return Description.NO_MATCH;
      } else if (path.getLeaf() instanceof MethodTree) {
        enclosingMethod = (MethodTree) path.getLeaf();
        break;
      } else {
        path = path.getParentPath();
      }
    }
    MethodSymbol enclosingMethodSym = ASTHelpers.getSymbol(enclosingMethod);
    if (enclosingMethodSym == null) {
      return Description.NO_MATCH;
    }

    if (!ASTHelpers.hasAnnotation(enclosingMethodSym, "dagger.Provides", state)
        || ASTHelpers.hasDirectAnnotationWithSimpleName(enclosingMethodSym, "Nullable")) {
      return Description.NO_MATCH;
    }

    Fix addNullableFix =
        SuggestedFix.builder()
            .prefixWith(enclosingMethod, "@Nullable\n")
            .addImport("javax.annotation.Nullable")
            .build();

    CatchTree enclosingCatch = ASTHelpers.findEnclosingNode(state.getPath(), CatchTree.class);
    if (enclosingCatch == null) {
      // If not in a catch block, suggest adding @Nullable first, then throwing an exception.
      Fix throwRuntimeExceptionFix =
          SuggestedFix.replace(returnTree, "throw new RuntimeException();");
      return buildDescription(returnTree)
          .addFix(addNullableFix)
          .addFix(throwRuntimeExceptionFix)
          .build();
    } else {
      // If in a catch block, suggest throwing an exception first, then adding @Nullable.
      String replacement =
          String.format("throw new RuntimeException(%s);", enclosingCatch.getParameter().getName());
      Fix throwRuntimeExceptionFix = SuggestedFix.replace(returnTree, replacement);
      return buildDescription(returnTree)
          .addFix(throwRuntimeExceptionFix)
          .addFix(addNullableFix)
          .build();
    }
  }
}
