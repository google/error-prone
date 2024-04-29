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
import com.google.errorprone.bugpatterns.BugChecker.AnnotatedTypeTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.WildcardTree;
import java.util.List;
import java.util.Optional;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Nullness annotations directly on wildcard types are interpreted differently by different"
            + " tools",
    severity = WARNING)
public class NullableWildcard extends BugChecker implements AnnotatedTypeTreeMatcher {
  @Override
  public Description matchAnnotatedType(AnnotatedTypeTree tree, VisitorState state) {
    Optional<Nullness> nullness = NullnessAnnotations.fromAnnotationTrees(tree.getAnnotations());
    if (nullness.isEmpty()) {
      return NO_MATCH;
    }
    ExpressionTree typeTree = tree.getUnderlyingType();
    if (!(typeTree instanceof WildcardTree)) {
      return NO_MATCH;
    }
    return describeMatch(tree, fix(tree.getAnnotations(), (WildcardTree) typeTree, state));
  }

  Fix fix(List<? extends AnnotationTree> annotations, WildcardTree tree, VisitorState state) {
    ImmutableList<AnnotationTree> existingAnnotations =
        NullnessAnnotations.annotationsRelevantToNullness(annotations);
    if (existingAnnotations.size() != 1) {
      return SuggestedFix.emptyFix();
    }
    AnnotationTree existingAnnotation = getOnlyElement(existingAnnotations);
    SuggestedFix.Builder fix = SuggestedFix.builder().delete(existingAnnotation);
    switch (tree.getKind()) {
      case EXTENDS_WILDCARD:
        Tree bound = tree.getBound();
        if (bound instanceof AnnotatedTypeTree
            && NullnessAnnotations.fromAnnotationTrees(((AnnotatedTypeTree) bound).getAnnotations())
                .isPresent()) {
          return SuggestedFix.emptyFix();
        }
        return fix.prefixWith(
                bound, String.format("%s ", state.getSourceForNode(existingAnnotation)))
            .build();
      case SUPER_WILDCARD:
        return SuggestedFix.emptyFix();
      case UNBOUNDED_WILDCARD:
        return fix.postfixWith(
                tree,
                String.format(" extends %s Object", state.getSourceForNode(existingAnnotation)))
            .build();
      default:
        throw new AssertionError(tree.getKind());
    }
  }
}
