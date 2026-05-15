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

package com.google.errorprone.matchers;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.symbolHasAnnotation;
import static com.google.errorprone.util.ASTHelpers.annotationsAmong;
import static com.google.errorprone.util.ASTHelpers.getDeclaredSymbol;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.util.Name;
import java.util.stream.Stream;

/** Utility constants and matchers related to dependency injection. */
public final class InjectMatchers {

  public static final Matcher<Tree> INSIDE_GUICE_MODULE =
      enclosingClass(isSubtypeOf("com.google.inject.Module"));

  private InjectMatchers() {} // no instantiation

  public static final String GUICE_PROVIDES_ANNOTATION = "com.google.inject.Provides";
  public static final String DAGGER_PROVIDES_ANNOTATION = "dagger.Provides";

  public static final ImmutableSet<String> PROVIDES_ANNOTATIONS =
      ImmutableSet.of(GUICE_PROVIDES_ANNOTATION, DAGGER_PROVIDES_ANNOTATION);

  public static final ImmutableSet<String> MULTIBINDINGS_ANNOTATIONS =
      ImmutableSet.of(
          "com.google.inject.multibindings.ProvidesIntoMap",
          "com.google.inject.multibindings.ProvidesIntoSet",
          "com.google.inject.multibindings.ProvidesIntoOptional");

  private static final Matcher<Tree> HAS_PROVIDES_ANNOTATION =
      annotations(
          AT_LEAST_ONE,
          anyOf(
              Streams.concat(
                      PROVIDES_ANNOTATIONS.stream(),
                      MULTIBINDINGS_ANNOTATIONS.stream(),
                      Stream.of(
                          "com.google.inject.throwingproviders.CheckedProvides",
                          "dagger.producers.Produces"))
                  .map(annotation -> isType(annotation))
                  .collect(toImmutableSet())));

  @SuppressWarnings("unchecked") // Safe contravariant cast
  public static <T extends Tree> Matcher<T> hasProvidesAnnotation() {
    return (Matcher<T>) HAS_PROVIDES_ANNOTATION;
  }

  public static final String ASSISTED_ANNOTATION = "com.google.inject.assistedinject.Assisted";
  public static final String ASSISTED_INJECT_ANNOTATION =
      "com.google.inject.assistedinject.AssistedInject";

  public static final String GUICE_INJECT_ANNOTATION = "com.google.inject.Inject";
  public static final String JAVAX_INJECT_ANNOTATION = "javax.inject.Inject";
  public static final String JAKARTA_INJECT_ANNOTATION = "jakarta.inject.Inject";

  public static final ImmutableSet<String> INJECT_ANNOTATIONS =
      ImmutableSet.of(GUICE_INJECT_ANNOTATION, JAVAX_INJECT_ANNOTATION, JAKARTA_INJECT_ANNOTATION);

  public static final Matcher<AnnotationTree> IS_APPLICATION_OF_GUICE_INJECT =
      new AnnotationType(GUICE_INJECT_ANNOTATION);

  public static final Matcher<AnnotationTree> IS_APPLICATION_OF_JAVAX_INJECT =
      new AnnotationType(JAVAX_INJECT_ANNOTATION);

  public static final Matcher<AnnotationTree> IS_APPLICATION_OF_JAKARTA_INJECT =
      new AnnotationType(JAKARTA_INJECT_ANNOTATION);

  public static final Matcher<AnnotationTree> IS_APPLICATION_OF_AT_INJECT =
      anyOf(
          IS_APPLICATION_OF_GUICE_INJECT,
          IS_APPLICATION_OF_JAVAX_INJECT,
          IS_APPLICATION_OF_JAKARTA_INJECT);

  public static final Matcher<Tree> HAS_INJECT_ANNOTATION =
      hasAnyOfAnnotations(
          ImmutableSet.of(
              GUICE_INJECT_ANNOTATION, JAVAX_INJECT_ANNOTATION, JAKARTA_INJECT_ANNOTATION));

  @SuppressWarnings("unchecked") // Safe contravariant cast
  public static <T extends Tree> Matcher<T> hasInjectAnnotation() {
    return (Matcher<T>) HAS_INJECT_ANNOTATION;
  }

  public static final String GUICE_SCOPE_ANNOTATION = "com.google.inject.ScopeAnnotation";
  public static final String JAVAX_SCOPE_ANNOTATION = "javax.inject.Scope";
  private static final String JAKARTA_SCOPE_ANNOTATION = "jakarta.inject.Scope";

  public static final ImmutableSet<String> SCOPE_ANNOTATIONS =
      ImmutableSet.of(GUICE_SCOPE_ANNOTATION, JAVAX_SCOPE_ANNOTATION, JAKARTA_SCOPE_ANNOTATION);

  public static final Matcher<AnnotationTree> IS_SCOPING_ANNOTATION =
      anyOf(
          SCOPE_ANNOTATIONS.stream()
              .map(annotation -> symbolHasAnnotation(annotation))
              .collect(toImmutableSet()));

  public static final Matcher<ClassTree> HAS_SCOPE_ANNOTATION =
      hasAnyOfAnnotations(SCOPE_ANNOTATIONS);

  public static final String GUICE_BINDING_ANNOTATION = "com.google.inject.BindingAnnotation";
  public static final String JAVAX_QUALIFIER_ANNOTATION = "javax.inject.Qualifier";
  private static final String JAKARTA_QUALIFIER_ANNOTATION = "jakarta.inject.Qualifier";

  public static final ImmutableSet<String> QUALIFIER_ANNOTATIONS =
      ImmutableSet.of(
          GUICE_BINDING_ANNOTATION, JAVAX_QUALIFIER_ANNOTATION, JAKARTA_QUALIFIER_ANNOTATION);

  public static final Matcher<AnnotationTree> IS_QUALIFIER_ANNOTATION =
      anyOf(
          QUALIFIER_ANNOTATIONS.stream()
              .map(annotation -> symbolHasAnnotation(annotation))
              .collect(toImmutableSet()));

  public static final Matcher<ClassTree> HAS_QUALIFIER_ANNOTATION =
      hasAnyOfAnnotations(QUALIFIER_ANNOTATIONS);

  public static final String GUICE_MAP_KEY_ANNOTATION = "com.google.inject.multibindings.MapKey";
  public static final String DAGGER_MAP_KEY_ANNOTATION = "dagger.MapKey";

  public static final Matcher<ClassTree> IS_DAGGER_COMPONENT =
      hasAnyOfAnnotations(
          ImmutableSet.of(
              "dagger.Component",
              "dagger.Subcomponent",
              "dagger.producers.ProductionComponent",
              "dagger.producers.ProductionSubcomponent",
              "dagger.hilt.DefineComponent"));

  public static final Matcher<ClassTree> IS_DAGGER_COMPONENT_OR_MODULE =
      anyOf(IS_DAGGER_COMPONENT, hasAnnotation("dagger.Module"));

  private static <T extends Tree> Matcher<T> hasAnyOfAnnotations(
      ImmutableSet<String> annotationClasses) {
    Supplier<ImmutableSet<Name>> name =
        VisitorState.memoize(
            state ->
                annotationClasses.stream()
                    .map(state::binaryNameFromClassname)
                    .collect(toImmutableSet()));
    return (T tree, VisitorState state) ->
        !annotationsAmong(getDeclaredSymbol(tree), name.get(state), state).isEmpty();
  }
}
