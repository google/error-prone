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

package com.google.errorprone.bugpatterns.inject.dagger;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.hasDefinitelyNullBranch;
import static com.google.errorprone.util.ASTHelpers.findEnclosingMethod;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import javax.inject.Inject;

/**
 * Bug checker for null-returning methods annotated with {@code @Provides} but not
 * {@code @Nullable}.
 */
@BugPattern(
    name = "DaggerProvidesNull",
    summary = "Dagger @Provides methods may not return null unless annotated with @Nullable",
    severity = ERROR)
public class ProvidesNull extends BugChecker implements ReturnTreeMatcher {

  // TODO(b/536946282): Remove flag after rollout.
  private final boolean checkDefinitelyNullBranch;

  @Inject
  ProvidesNull(ErrorProneFlags flags) {
    this.checkDefinitelyNullBranch =
        flags.getBoolean("ProvidesNull:CheckDefinitelyNullBranch").orElse(true);
  }

  /**
   * Matches explicit "return null" statements in methods annotated with {@code @Provides} but not
   * {@code @Nullable}. Suggests either annotating the method with {@code @Nullable} or throwing a
   * {@link RuntimeException} instead.
   */
  // TODO(eaftan): Use nullness dataflow analysis when it's ready
  @Override
  public Description matchReturn(ReturnTree returnTree, VisitorState state) {
    ExpressionTree returnExpression = returnTree.getExpression();
    if (returnExpression == null) {
      return Description.NO_MATCH;
    }
    if (checkDefinitelyNullBranch) {
      if (!hasDefinitelyNullBranch(
          returnExpression,
          /* definitelyNullVars= */ ImmutableSet.of(),
          /* varsProvenNullByParentIf= */ ImmutableSet.of(),
          state)) {
        return Description.NO_MATCH;
      }
    } else if (returnExpression.getKind() != Kind.NULL_LITERAL) {
      return Description.NO_MATCH;
    }

    MethodTree enclosingMethod = findEnclosingMethod(state);
    if (enclosingMethod == null) {
      return Description.NO_MATCH;
    }
    MethodSymbol enclosingMethodSym = ASTHelpers.getSymbol(enclosingMethod);

    // Method is not annotated as Provides -> No match
    if (!ASTHelpers.hasAnnotation(enclosingMethodSym, "dagger.Provides", state)) {
      return Description.NO_MATCH;
    }
    // Method is annotated as Nullable -> No match
    if (ASTHelpers.hasDirectAnnotationWithSimpleName(enclosingMethodSym, "Nullable")) {
      return Description.NO_MATCH;
    }

    /*
     * Dagger has support for type-use annotations only under newer compilers with a flag enabled:
     * https://github.com/google/dagger/releases/tag/dagger-2.60.
     *
     * To be safe, we suggest a fix that uses javax.annotations.Nullable.
     *
     * TODO: b/117251022 - Once javac and Dagger support type-use annotations more widely, generate
     * a fix by using fixByAddingNullableAnnotationToReturnType, which uses whichever Nullable the
     * user is already using and which supports JSpecify (and which might universally prefer it by
     * default by then) and other type-use annotations.
     */
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
