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
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.InjectMatchers.ASSISTED_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.ASSISTED_INJECT_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.DAGGER_PRODUCES_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.DAGGER_PROVIDES_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_BINDING_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.JAVAX_QUALIFIER_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.TEST_CASE;
import static com.google.errorprone.matchers.JUnitMatchers.isJUnit4TestRunnerOfType;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.hasExplicitSource;
import static com.google.errorprone.util.ASTHelpers.isCanonicalRecordConstructor;
import static com.google.errorprone.util.ASTHelpers.isRecord;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.AnnotationNames.AUTO_FACTORY_ANNOTATION;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ClassName;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.InjectMatchers;
import com.google.errorprone.matchers.MultiMatcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.VariableTree;
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
    return deleteAnnotations(annotations, state);
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
        if (INJECTION_FIELDS.stream().anyMatch(ip -> hasAnnotation(tree, ip, state))
            || tree.getModifiers().getAnnotations().stream()
                .anyMatch(anno ->
                    INJECTION_PREFIXES.stream()
                        .anyMatch(p -> getSymbol(anno).getSimpleName().toString().startsWith(p)))) {
          return NO_MATCH;
        }
        if (isRecord(symbol)) {
          return NO_MATCH;
        }
      }
      case PARAMETER -> {
        if (state.getPath().getParentPath().getLeaf() instanceof LambdaExpressionTree) {
          // Annotations on lambda parameters are never meaningful.
          return deleteAnnotations(annotations, state);
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
        // CDI observer methods use qualifiers on both the event and additional injected parameters.
        if (method.getParameters().stream()
            .anyMatch(
                parameter ->
                    hasAnnotation(parameter, "javax.enterprise.event.Observes", state)
                        || hasAnnotation(
                            parameter, "javax.enterprise.event.ObservesAsync", state))) {
          return NO_MATCH;
        }
        if (method.getModifiers().getAnnotations().stream()
            .anyMatch(
                anno -> getSymbol(anno).getSimpleName().toString().startsWith(PROVIDES_PREFIX))) {
          return NO_MATCH;
        }
        if (SUPERTYPES_DENOTING_INJECTION_POINTS.stream()
            .anyMatch(st -> isSubtype(methodSymbol.owner.type, st, state))) {
          return NO_MATCH;
        }
        var enclosingClass = state.findEnclosing(ClassTree.class);
        if (TEST_CASE.matches(method, state) && HAS_JUKITO_RUNNER.matches(enclosingClass, state)) {
          return NO_MATCH;
        }

        if (isCanonicalRecordConstructor(methodSymbol, state)) {
          return NO_MATCH;
        }
        if (CLASS_ANNOTATIONS_EXEMPTING_METHODS.stream()
            .anyMatch(anno -> hasAnnotation(enclosingClass, anno, state))) {
          return NO_MATCH;
        }
      }
      default -> {
        // fall out
      }
    }
    return deleteAnnotations(annotations, state);
  }

  private static final MultiMatcher<ClassTree, AnnotationTree> HAS_JUKITO_RUNNER =
      annotations(
          AT_LEAST_ONE,
          hasArgumentWithValue(
              "value", isJUnit4TestRunnerOfType(ImmutableSet.of("org.jukito.JukitoRunner"))));

  private Description deleteAnnotations(
      ImmutableList<AnnotationTree> annotations, VisitorState state) {
    // annotations propagated to synthetic record constructors don't have source positions
    ImmutableList<AnnotationTree> explicitAnnotations =
        annotations.stream()
            .filter(annotation -> hasExplicitSource(annotation, state))
            .collect(toImmutableList());
    if (explicitAnnotations.isEmpty()) {
      return NO_MATCH;
    }
    boolean plural = explicitAnnotations.size() > 1;
    return buildDescription(explicitAnnotations.getFirst())
        .addFix(explicitAnnotations.stream().map(SuggestedFix::delete).collect(mergeFixes()))
        .setMessage(
            String.format(
                "Qualifier annotation%s %s %s no effect here",
                plural ? "s" : "",
                explicitAnnotations.stream()
                    .map(anno -> getSymbol(anno).getQualifiedName().toString())
                    .collect(joining(", ", "@", "")),
                plural ? "have" : "has"))
        .build();
  }

  private static ImmutableList<AnnotationTree> getQualifiers(
      ModifiersTree modifiers, VisitorState state) {
    return modifiers.getAnnotations().stream()
        .filter(anno ->
            QUALIFIERS.stream().anyMatch(q -> hasAnnotation(getSymbol(anno), q, state))
                && IGNORED_ANNOTATIONS.stream()
                    .noneMatch(a -> getSymbol(anno).equals(a.symbol(state))))
        .collect(toImmutableList());
  }

  private static final ImmutableSet<ClassName> QUALIFIERS =
      ImmutableSet.of(GUICE_BINDING_ANNOTATION, JAVAX_QUALIFIER_ANNOTATION);

  private static final ImmutableSet<ClassName> SUPERTYPES_DENOTING_INJECTION_POINTS =
      ImmutableSet.of(ClassName.of("com.google.apps.framework.producers.GraphWrapper"));

  /**
   * Method annotations denoting that the method provides bindings, so qualifiers on the return type
   * are OK.
   */
  private static final ImmutableSet<ClassName> PROVIDER_METHODS =
      ImmutableSet.of(
          // keep-sorted start
          ClassName.of("com.google.inject.throwingproviders.CheckedProvides"),
          ClassName.of("dagger.Binds"),
          ClassName.of("dagger.BindsInstance"),
          ClassName.of("dagger.BindsOptionalOf"),
          ClassName.of("dagger.multibindings.Multibinds"),
          DAGGER_PRODUCES_ANNOTATION,
          DAGGER_PROVIDES_ANNOTATION
          // keep-sorted end
          );

  private static final String PROVIDES_PREFIX = "Provides";

  /** Annotations for methods which have parameters injected into them. */
  private static final ImmutableSet<ClassName> INJECTION_METHODS =
      concat(
              concat(InjectMatchers.INJECT_ANNOTATIONS.stream()).map(ClassName::of),
              Stream.of(
                  // keep-sorted start
                  ASSISTED_INJECT_ANNOTATION,
                  AUTO_FACTORY_ANNOTATION,
                  ClassName.of("com.google.common.inject.components.OtherRequiredBindings"),
                  ClassName.of("dagger.assisted.AssistedInject")
                  // keep-sorted end
                  ),
              PROVIDER_METHODS.stream())
          .collect(toImmutableSet());

  /** Annotations for fields which can have qualifiers. */
  private static final ImmutableSet<ClassName> INJECTION_FIELDS = ImmutableSet.of();

  /** Prefixes for annotations on variables which can have qualifiers. */
  private static final ImmutableSet<String> INJECTION_PREFIXES = ImmutableSet.of("Bind", "Inject");

  private static final ImmutableSet<ClassName> CLASS_ANNOTATIONS_EXEMPTING_METHODS =
      ImmutableSet.of(
          // keep-sorted start
          AUTO_FACTORY_ANNOTATION,
          ClassName.of("dagger.Component"),
          ClassName.of("dagger.Component.Builder"),
          ClassName.of("dagger.Component.Factory"),
          ClassName.of("dagger.Subcomponent"),
          ClassName.of("dagger.Subcomponent.Builder"),
          ClassName.of("dagger.Subcomponent.Factory"),
          ClassName.of("dagger.hilt.EntryPoint"),
          ClassName.of("dagger.producers.ProductionComponent"),
          ClassName.of("dagger.producers.ProductionComponent.Builder"),
          ClassName.of("dagger.producers.ProductionComponent.Factory"),
          ClassName.of("dagger.producers.ProductionSubcomponent"),
          ClassName.of("dagger.producers.ProductionSubcomponent.Builder"),
          ClassName.of("dagger.producers.ProductionSubcomponent.Factory")
          // keep-sorted end
          );

  private static final ImmutableSet<ClassName> IGNORED_ANNOTATIONS =
      ImmutableSet.of(
          // keep-sorted start
          ASSISTED_ANNOTATION
          // keep-sorted end
          );
}
