/*
 * Copyright 2025 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Streams.concat;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFix.mergeFixes;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.stream.Stream;

/** A BugPattern; see the summary. */
@BugPattern(summary = "A qualifier annotation has no effect here.", severity = WARNING)
public final class UnnecessaryQualifier extends BugChecker
    implements MethodTreeMatcher, VariableTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    ImmutableList<AnnotationTree> annotations = getQualifiers(tree.getModifiers(), state);
    if (annotations.isEmpty()) {
      return NO_MATCH;
    }
    if (PROVIDER_METHODS.stream().anyMatch(ip -> hasAnnotation(tree, ip, state))) {
      return NO_MATCH;
    }
    if (tree.getModifiers().getAnnotations().stream()
        .anyMatch(anno -> getSymbol(anno).getSimpleName().toString().startsWith(PROVIDES_PREFIX))) {
      return NO_MATCH;
    }

    var enclosingClass = state.findEnclosing(ClassTree.class);
    if (getSymbol(enclosingClass).isInterface()) {
      // This is a sad admission of failure, and also not foolproof. Dagger dependencies can be
      // declared in interfaces with no annotations to let us tell, or components can have
      // innocent-looking supertypes.
      return NO_MATCH;
    }
    if (CLASS_ANNOTATIONS_EXEMPTING_METHODS.stream()
        .anyMatch(anno -> hasAnnotation(enclosingClass, anno, state))) {
      return NO_MATCH;
    }
    return deleteAnnotations(annotations);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    ImmutableList<AnnotationTree> annotations = getQualifiers(tree.getModifiers(), state);
    if (annotations.isEmpty()) {
      return NO_MATCH;
    }
    var symbol = getSymbol(tree);
    switch (symbol.getKind()) {
      case FIELD -> {
        if (INJECTION_FIELDS.stream().anyMatch(ip -> hasAnnotation(tree, ip, state))) {
          return NO_MATCH;
        }
      }
      case PARAMETER -> {
        if (state.getPath().getParentPath().getLeaf() instanceof LambdaExpressionTree) {
          // Annotations on lambda parameters are never meaningful.
          return deleteAnnotations(annotations);
        }
        var method = state.findEnclosing(MethodTree.class);
        if (method == null) {
          return NO_MATCH;
        }
        var methodSymbol = getSymbol(method);
        if (methodSymbol != symbol.owner) {
          return NO_MATCH;
        }
        if (INJECTION_METHODS.stream().anyMatch(ip -> hasAnnotation(method, ip, state))) {
          return NO_MATCH;
        }
        if (method.getModifiers().getAnnotations().stream()
            .anyMatch(
                anno -> getSymbol(anno).getSimpleName().toString().startsWith(PROVIDES_PREFIX))) {
          return NO_MATCH;
        }
        if (SUPERTYPES_DENOTING_INJECTION_POINTS.get(state).stream()
            .anyMatch(st -> isSubtype(methodSymbol.owner.type, st, state))) {
          return NO_MATCH;
        }
        var enclosingClass = state.findEnclosing(ClassTree.class);
        if (CLASS_ANNOTATIONS_EXEMPTING_METHODS.stream()
            .anyMatch(anno -> hasAnnotation(enclosingClass, anno, state))) {
          return NO_MATCH;
        }
      }
      default -> {
        // fall out
      }
    }
    return deleteAnnotations(annotations);
  }

  private Description deleteAnnotations(ImmutableList<AnnotationTree> annotations) {
    return describeMatch(
        annotations.get(0), annotations.stream().map(SuggestedFix::delete).collect(mergeFixes()));
  }

  private static ImmutableList<AnnotationTree> getQualifiers(
      ModifiersTree modifiers, VisitorState state) {
    return modifiers.getAnnotations().stream()
        .filter(
            anno ->
                QUALIFIERS.stream().anyMatch(q -> hasAnnotation(getSymbol(anno), q, state))
                    && !IGNORED_ANNOTATIONS.contains(getSymbol(anno).getQualifiedName().toString()))
        .collect(toImmutableList());
  }

  private static final ImmutableSet<String> QUALIFIERS =
      ImmutableSet.of("com.google.inject.BindingAnnotation", "javax.inject.Qualifier");

  private static final Supplier<ImmutableSet<Type>> SUPERTYPES_DENOTING_INJECTION_POINTS =
      VisitorState.memoize(
          s ->
              Stream.of(s.getTypeFromString("com.google.apps.framework.producers.GraphWrapper"))
                  .filter(x -> x != null)
                  .collect(toImmutableSet()));

  /**
   * Method annotations denoting that the method provides bindings, so qualifiers on the return type
   * are OK.
   */
  private static final ImmutableSet<String> PROVIDER_METHODS =
      ImmutableSet.of(
          // keep-sorted start
          "com.google.inject.throwingproviders.CheckedProvides",
          "dagger.Binds",
          "dagger.BindsInstance",
          "dagger.BindsOptionalOf",
          "dagger.Provides",
          "dagger.multibindings.Multibinds",
          "dagger.producers.Produces"
          // keep-sorted end
          );

  private static final String PROVIDES_PREFIX = "Provides";

  /** Annotations for methods which have parameters injected into them. */
  private static final ImmutableSet<String> INJECTION_METHODS =
      concat(
              Stream.of(
                  // keep-sorted start
                  "com.google.auto.factory.AutoFactory",
                  "com.google.inject.Inject",
                  "dagger.assisted.AssistedInject",
                  "jakarta.inject.Inject",
                  "javax.inject.Inject"
                  // keep-sorted end
                  ),
              PROVIDER_METHODS.stream())
          .collect(toImmutableSet());

  /** Annotations for fields which can have qualifiers. */
  private static final ImmutableSet<String> INJECTION_FIELDS =
      ImmutableSet.of(
          // keep-sorted start
          "com.google.inject.Inject",
          "dagger.Binds",
          "dagger.BindsInstance",
          "dagger.hilt.android.testing.BindElementsIntoSet",
          "dagger.hilt.android.testing.BindValue",
          "dagger.hilt.android.testing.BindValueIntoMap",
          "dagger.hilt.android.testing.BindValueIntoSet",
          "jakarta.inject.Inject",
          "javax.inject.Inject"
          // keep-sorted end
          );

  private static final ImmutableSet<String> CLASS_ANNOTATIONS_EXEMPTING_METHODS =
      ImmutableSet.of(
          // keep-sorted start
          "com.google.auto.factory.AutoFactory",
          "dagger.Component",
          "dagger.Component.Builder",
          "dagger.Component.Factory",
          "dagger.Subcomponent",
          "dagger.Subcomponent.Builder",
          "dagger.Subcomponent.Builder.Factory",
          "dagger.hilt.EntryPoint",
          "dagger.producers.ProductionComponent",
          "dagger.producers.ProductionComponent.Builder",
          "dagger.producers.ProductionComponent.Factory",
          "dagger.producers.ProductionSubcomponent",
          "dagger.producers.ProductionSubcomponent.Builder",
          "dagger.producers.ProductionSubcomponent.Factory"
          // keep-sorted end
          );

  private static final ImmutableSet<String> IGNORED_ANNOTATIONS =
      ImmutableSet.of(
          // keep-sorted start
          "com.google.inject.assistedinject.Assisted"
          // keep-sorted end
          );
}
