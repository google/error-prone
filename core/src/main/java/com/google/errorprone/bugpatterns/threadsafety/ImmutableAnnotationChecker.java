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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getGeneratedBy;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.Immutable;
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

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "ImmutableAnnotationChecker",
    altNames = "Immutable",
    category = JDK,
    summary = "Annotations should always be immutable",
    severity = WARNING,
    tags = StandardTags.LIKELY_ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class ImmutableAnnotationChecker extends BugChecker implements ClassTreeMatcher {

  public static final String ANNOTATED_ANNOTATION_MESSAGE =
      "annotations are immutable by default; annotating them with"
          + " @com.google.errorprone.annotations.Immutable is unnecessary";

  private static final ImmutableSet<String> PROCESSOR_BLACKLIST =
      ImmutableSet.of(
          "com.google.auto.value.processor.AutoAnnotationProcessor"
          );

  private final WellKnownMutability wellKnownMutability;

  @Deprecated // Used reflectively, but you should pass in ErrorProneFlags to get custom mutability
  public ImmutableAnnotationChecker() {
    this(ErrorProneFlags.empty());
  }

  public ImmutableAnnotationChecker(ErrorProneFlags flags) {
    this.wellKnownMutability = WellKnownMutability.fromFlags(flags);
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol symbol = getSymbol(tree);
    if (symbol == null
        || symbol.isAnnotationType()
        || !WellKnownMutability.isAnnotation(state, symbol.type)) {
      return NO_MATCH;
    }
    if (!Collections.disjoint(getGeneratedBy(symbol, state), PROCESSOR_BLACKLIST)) {
      return NO_MATCH;
    }

    if (ASTHelpers.hasAnnotation(symbol, Immutable.class, state)) {
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
        new ImmutableAnalysis(
                this, state, wellKnownMutability, ImmutableSet.of(Immutable.class.getName()))
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
