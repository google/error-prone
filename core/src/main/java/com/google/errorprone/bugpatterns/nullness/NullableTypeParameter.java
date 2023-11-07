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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.TypeParameterTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import java.util.List;
import java.util.Optional;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Nullness annotations directly on type parameters are interpreted differently by different"
            + " tools",
    severity = WARNING)
public class NullableTypeParameter extends BugChecker implements TypeParameterTreeMatcher {

  @Override
  public Description matchTypeParameter(TypeParameterTree tree, VisitorState state) {
    Optional<Nullness> nullness = NullnessAnnotations.fromAnnotationTrees(tree.getAnnotations());
    if (nullness.isEmpty()) {
      return NO_MATCH;
    }
    return describeMatch(tree, fix(tree.getAnnotations(), tree, state));
  }

  Fix fix(List<? extends AnnotationTree> annotations, TypeParameterTree tree, VisitorState state) {
    ImmutableList<AnnotationTree> existingAnnotations =
        NullnessAnnotations.annotationsRelevantToNullness(annotations);
    if (existingAnnotations.size() != 1) {
      return SuggestedFix.emptyFix();
    }
    AnnotationTree existingAnnotation = getOnlyElement(existingAnnotations);
    SuggestedFix.Builder fix = SuggestedFix.builder().delete(existingAnnotation);
    List<? extends Tree> bounds = tree.getBounds();
    if (bounds.stream()
        .anyMatch(
            b ->
                b instanceof AnnotatedTypeTree
                    && NullnessAnnotations.fromAnnotationTrees(
                            ((AnnotatedTypeTree) b).getAnnotations())
                        .isPresent())) {
      return SuggestedFix.emptyFix();
    }
    if (bounds.isEmpty()) {
      return fix.postfixWith(
              tree, String.format(" extends %s Object", state.getSourceForNode(existingAnnotation)))
          .build();
    } else {
      String prefix = String.format("%s ", state.getSourceForNode(existingAnnotation));
      bounds.forEach(bound -> fix.prefixWith(bound, prefix));
      return fix.build();
    }
  }
}
