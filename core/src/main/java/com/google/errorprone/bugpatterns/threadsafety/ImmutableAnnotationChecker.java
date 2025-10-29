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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getGeneratedBy;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.AnnotationNames.IMMUTABLE_ANNOTATION;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ThreadSafety.Violation;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.util.Collections;
import java.util.Optional;
import javax.inject.Inject;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "ImmutableAnnotationChecker",
    altNames = "Immutable",
    summary = "Annotations should always be immutable",
    severity = WARNING,
    tags = StandardTags.LIKELY_ERROR)
public class ImmutableAnnotationChecker extends BugChecker implements ClassTreeMatcher {

  public static final String ANNOTATED_ANNOTATION_MESSAGE =
      "annotations are immutable by default; annotating them with"
          + " @com.google.errorprone.annotations.Immutable is unnecessary";

  private static final ImmutableSet<String> IGNORED_PROCESSORS =
      ImmutableSet.of("com.google.auto.value.processor.AutoAnnotationProcessor");

  private final ImmutableAnalysis.Factory immutableAnalysisFactory;

  @Inject
  ImmutableAnnotationChecker(ImmutableAnalysis.Factory immutableAnalysisFactory) {
    this.immutableAnalysisFactory = immutableAnalysisFactory;
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol symbol = getSymbol(tree);
    if (symbol.isAnnotationType() || !WellKnownMutability.isAnnotation(state, symbol.type)) {
      return NO_MATCH;
    }
    if (!Collections.disjoint(getGeneratedBy(symbol), IGNORED_PROCESSORS)) {
      return NO_MATCH;
    }

    if (hasAnnotation(symbol, IMMUTABLE_ANNOTATION, state)) {
      AnnotationTree annotation =
          ASTHelpers.getAnnotationWithSimpleName(tree.getModifiers().getAnnotations(), "Immutable");
      if (annotation != null) {
        state.reportMatch(
            buildDescription(annotation)
                .setMessage(ANNOTATED_ANNOTATION_MESSAGE)
                .addFix(SuggestedFix.delete(annotation))
                .build());
      } else {
        state.reportMatch(buildDescription(tree).setMessage(ANNOTATED_ANNOTATION_MESSAGE).build());
      }
    }

    Violation info =
        immutableAnalysisFactory
            .create(this::isSuppressed, state, ImmutableSet.of(IMMUTABLE_ANNOTATION))
            .checkForImmutability(
                Optional.of(tree), ImmutableSet.of(), getType(tree), this::describeClass);

    if (!info.isPresent()) {
      return NO_MATCH;
    }
    return describeClass(tree, info).build();
  }

  Description.Builder describeClass(Tree tree, Violation info) {
    String message = "annotations should be immutable: " + info.message();
    return buildDescription(tree).setMessage(message);
  }
}
