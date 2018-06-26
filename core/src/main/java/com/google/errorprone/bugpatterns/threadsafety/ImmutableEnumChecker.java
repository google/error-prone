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
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
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
import java.util.Optional;
import java.util.stream.Stream;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "ImmutableEnumChecker",
    altNames = "Immutable",
    category = JDK,
    summary = "Enums should always be immutable",
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class ImmutableEnumChecker extends BugChecker implements ClassTreeMatcher {

  public static final String ANNOTATED_ENUM_MESSAGE =
      "enums are immutable by default; annotating them with"
          + " @com.google.errorprone.annotations.Immutable is unnecessary";

  private final WellKnownMutability wellKnownMutability;

  @Deprecated // Used reflectively, but you should pass in ErrorProneFlags to get custom mutability
  public ImmutableEnumChecker() {
    this(ErrorProneFlags.empty());
  }

  public ImmutableEnumChecker(ErrorProneFlags flags) {
    this.wellKnownMutability = WellKnownMutability.fromFlags(flags);
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol symbol = getSymbol(tree);
    if (symbol == null || !symbol.isEnum()) {
      return NO_MATCH;
    }

    if (ASTHelpers.hasAnnotation(symbol, Immutable.class, state)
        && !implementsImmutableInterface(symbol)) {
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
        new ImmutableAnalysis(
                this, state, wellKnownMutability, ImmutableSet.of(Immutable.class.getName()))
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

  private static boolean implementsImmutableInterface(ClassSymbol symbol) {
    return Streams.concat(symbol.getInterfaces().stream(), Stream.of(symbol.getSuperclass()))
        .anyMatch(supertype -> supertype.asElement().getAnnotation(Immutable.class) != null);
  }
}
