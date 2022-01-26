/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.predicates.TypePredicates;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.Optional;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "AnnotationMirror#toString doesn't use fully qualified type names, prefer auto-common's"
            + " AnnotationMirrors#toString",
    severity = SUGGESTION)
public class AnnotationMirrorToString extends AbstractToString {

  private static final TypePredicate TYPE_PREDICATE =
      TypePredicates.isExactType("javax.lang.model.element.AnnotationMirror");

  @Override
  protected TypePredicate typePredicate() {
    return TYPE_PREDICATE;
  }

  @Override
  protected Optional<Fix> implicitToStringFix(ExpressionTree tree, VisitorState state) {
    return fix(tree, tree, state);
  }

  @Override
  protected Optional<Fix> toStringFix(Tree parent, ExpressionTree tree, VisitorState state) {
    return fix(parent, tree, state);
  }

  private static Optional<Fix> fix(Tree replace, Tree with, VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    return Optional.of(
        fix.replace(
                replace,
                String.format(
                    "%s.toString(%s)",
                    qualifyType(state, fix, "com.google.auto.common.AnnotationMirrors"),
                    state.getSourceForNode(with)))
            .build());
  }
}
