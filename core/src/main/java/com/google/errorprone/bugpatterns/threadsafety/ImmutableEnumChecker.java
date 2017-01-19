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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ImmutableAnalysis.Violation;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "ImmutableEnumChecker",
  altNames = "Immutable",
  category = JDK,
  summary = "Enums should always be immutable",
  severity = WARNING
)
public class ImmutableEnumChecker extends BugChecker implements ClassTreeMatcher {

  public static final String ANNOTATED_ENUM_MESSAGE =
      "enums are immutable by default; annotating them with @Immutable is unnecessary";

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
                this,
                state,
                "enums should be immutable, and cannot have non-final fields",
                "enums should only have immutable fields")
            .checkForImmutability(Optional.of(tree), ImmutableSet.of(), getType(tree));

    if (!info.isPresent()) {
      return NO_MATCH;
    }

    String message = "enums should be immutable: " + info.message();
    return buildDescription(tree).setMessage(message).build();
  }

  private static boolean implementsImmutableInterface(ClassSymbol symbol) {
    return symbol
        .getInterfaces()
        .stream()
        .anyMatch(iface -> iface.asElement().getAnnotation(Immutable.class) != null);
  }
}
