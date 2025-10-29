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
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.AnnotationNames.IMMUTABLE_ANNOTATION;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
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
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "ImmutableEnumChecker",
    altNames = "Immutable",
    summary = "Enums should always be immutable",
    severity = WARNING)
public class ImmutableEnumChecker extends BugChecker implements ClassTreeMatcher {

  public static final String ANNOTATED_ENUM_MESSAGE =
      "enums are immutable by default; annotating them with"
          + " @com.google.errorprone.annotations.Immutable is unnecessary";

  private final ImmutableAnalysis.Factory immutableAnalysisFactory;

  @Inject
  ImmutableEnumChecker(ImmutableAnalysis.Factory immutableAnalysisFactory) {
    this.immutableAnalysisFactory = immutableAnalysisFactory;
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol symbol = getSymbol(tree);
    if (!symbol.isEnum()) {
      return NO_MATCH;
    }

    if (hasAnnotation(symbol, IMMUTABLE_ANNOTATION, state)
        && !implementsExemptInterface(symbol, state)) {
      AnnotationTree annotation =
          ASTHelpers.getAnnotationWithSimpleName(tree.getModifiers().getAnnotations(), "Immutable");
      if (annotation != null) {
        state.reportMatch(
            buildDescription(annotation)
                .setMessage(ANNOTATED_ENUM_MESSAGE)
                .addFix(SuggestedFix.delete(annotation))
                .build());
      } else {
        state.reportMatch(buildDescription(tree).setMessage(ANNOTATED_ENUM_MESSAGE).build());
      }
    }

    Violation info =
        immutableAnalysisFactory
            .create(this::isSuppressed, state, ImmutableSet.of(IMMUTABLE_ANNOTATION))
            .checkForImmutability(
                Optional.of(tree), ImmutableSet.of(), getType(tree), this::describe);

    if (!info.isPresent()) {
      return NO_MATCH;
    }

    return describe(tree, info).build();
  }

  private Description.Builder describe(Tree tree, Violation info) {
    String message = "enums should be immutable: " + info.message();
    return buildDescription(tree).setMessage(message);
  }

  private static boolean implementsExemptInterface(ClassSymbol symbol, VisitorState state) {
    return Streams.concat(symbol.getInterfaces().stream(), Stream.of(symbol.getSuperclass()))
        .anyMatch(supertype -> hasExemptAnnotation(supertype.tsym, state));
  }

  private static final ImmutableSet<String> EXEMPT_ANNOTATIONS =
      ImmutableSet.of(IMMUTABLE_ANNOTATION);

  private static boolean hasExemptAnnotation(Symbol symbol, VisitorState state) {
    return EXEMPT_ANNOTATIONS.stream()
        .anyMatch(annotation -> hasAnnotation(symbol, annotation, state));
  }
}
