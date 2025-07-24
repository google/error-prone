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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations.annotationsAreAmbiguous;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations.annotationsRelevantToNullness;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotatedTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.Collection;
import javax.lang.model.element.AnnotationMirror;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "This type use has conflicting nullness annotations", severity = WARNING)
public class MultipleNullnessAnnotations extends BugChecker
    implements AnnotatedTypeTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {
  @Override
  public Description matchAnnotatedType(AnnotatedTypeTree tree, VisitorState state) {
    return match(tree, getType(tree).getAnnotationMirrors());
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol sym = getSymbol(tree);
    return match(tree, sym, sym.getReturnType());
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    VarSymbol sym = getSymbol(tree);
    return match(tree, sym, sym.asType());
  }

  private Description match(Tree tree, Symbol symbol, Type type) {
    if (type == null) {
      return NO_MATCH;
    }
    return match(
        tree,
        ImmutableSet.<AnnotationMirror>builder()
            .addAll(symbol.getAnnotationMirrors())
            .addAll(type.getAnnotationMirrors())
            .build());
  }

  private Description match(Tree tree, Collection<? extends AnnotationMirror> annotations) {
    if (!annotationsAreAmbiguous(annotations)) {
      return NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage(
            annotationsRelevantToNullness(annotations).stream()
                .map(a -> a.getAnnotationType().asElement().toString())
                .collect(joining(", ", "This type use has conflicting nullness annotations: ", "")))
        .build();
  }
}
