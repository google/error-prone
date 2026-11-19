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
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasAnyAnnotationOf;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.ClassName;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import java.util.stream.Stream;

/** Utility constants and matchers related to dependency injection. */
public final class InjectMatchers {

  public static final ClassName GUICE_MODULE = ClassName.of("com.google.inject.Module");
  public static final ClassName GUICE_ABSTRACT_MODULE =
      ClassName.of("com.google.inject.AbstractModule");
  public static final ClassName GUICE_BIND_ANNOTATION =
      ClassName.of("com.google.inject.testing.fieldbinder.Bind");

  public static final Matcher<Tree> INSIDE_GUICE_MODULE = enclosingClass(isSubtypeOf(GUICE_MODULE));

  private InjectMatchers() {} // no instantiation

  public static final ClassName GUICE_PROVIDES_ANNOTATION =
      ClassName.of("com.google.inject.Provides");
  public static final ClassName DAGGER_PROVIDES_ANNOTATION = ClassName.of("dagger.Provides");
  public static final ClassName DAGGER_PRODUCES_ANNOTATION =
      ClassName.of("dagger.producers.Produces");

  public static final ImmutableSet<String> PROVIDES_ANNOTATIONS =
      ImmutableSet.of(GUICE_PROVIDES_ANNOTATION.toString(), DAGGER_PROVIDES_ANNOTATION.toString());

  public static final ImmutableSet<String> MULTIBINDINGS_ANNOTATIONS =
      ImmutableSet.of(
          "com.google.inject.multibindings.ProvidesIntoMap",
          "com.google.inject.multibindings.ProvidesIntoSet",
          "com.google.inject.multibindings.ProvidesIntoOptional");

  private static final Matcher<Tree> HAS_PROVIDES_ANNOTATION =
      hasAnyAnnotationOf(
          Streams.concat(
                  Stream.of(
                      GUICE_PROVIDES_ANNOTATION,
                      DAGGER_PROVIDES_ANNOTATION,
                      DAGGER_PRODUCES_ANNOTATION,
                      ClassName.of("com.google.inject.throwingproviders.CheckedProvides")),
                  MULTIBINDINGS_ANNOTATIONS.stream().map(ClassName::of))
              .collect(toImmutableSet()));

  @SuppressWarnings("unchecked") // Safe contravariant cast
  public static <T extends Tree> Matcher<T> hasProvidesAnnotation() {
    return (Matcher<T>) HAS_PROVIDES_ANNOTATION;
  }

  public static final ClassName ASSISTED_ANNOTATION =
      ClassName.of("com.google.inject.assistedinject.Assisted");
  public static final ClassName ASSISTED_INJECT_ANNOTATION =
      ClassName.of("com.google.inject.assistedinject.AssistedInject");

  public static final ClassName GUICE_INJECT_ANNOTATION = ClassName.of("com.google.inject.Inject");
  public static final ClassName JAVAX_INJECT_ANNOTATION = ClassName.of("javax.inject.Inject");
  public static final ClassName JAKARTA_INJECT_ANNOTATION = ClassName.of("jakarta.inject.Inject");

  public static final ImmutableSet<String> INJECT_ANNOTATIONS =
      ImmutableSet.of(
          GUICE_INJECT_ANNOTATION.toString(),
          JAVAX_INJECT_ANNOTATION.toString(),
          JAKARTA_INJECT_ANNOTATION.toString());

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
      hasAnyAnnotationOf(
          ImmutableSet.of(
              GUICE_INJECT_ANNOTATION, JAVAX_INJECT_ANNOTATION, JAKARTA_INJECT_ANNOTATION));

  @SuppressWarnings("unchecked") // Safe contravariant cast
  public static <T extends Tree> Matcher<T> hasInjectAnnotation() {
    return (Matcher<T>) HAS_INJECT_ANNOTATION;
  }

  public static final ClassName GUICE_SCOPE_ANNOTATION =
      ClassName.of("com.google.inject.ScopeAnnotation");
  public static final ClassName JAVAX_SCOPE_ANNOTATION = ClassName.of("javax.inject.Scope");
  private static final ClassName JAKARTA_SCOPE_ANNOTATION = ClassName.of("jakarta.inject.Scope");

  public static final ImmutableSet<ClassName> SCOPE_ANNOTATIONS =
      ImmutableSet.of(GUICE_SCOPE_ANNOTATION, JAVAX_SCOPE_ANNOTATION, JAKARTA_SCOPE_ANNOTATION);

  public static final Matcher<AnnotationTree> IS_SCOPING_ANNOTATION =
      anyOf(
          SCOPE_ANNOTATIONS.stream().map(Matchers::symbolHasAnnotation).collect(toImmutableSet()));

  public static final Matcher<ClassTree> HAS_SCOPE_ANNOTATION =
      hasAnyAnnotationOf(SCOPE_ANNOTATIONS);

  public static final ClassName GUICE_BINDING_ANNOTATION =
      ClassName.of("com.google.inject.BindingAnnotation");
  public static final ClassName JAVAX_QUALIFIER_ANNOTATION = ClassName.of("javax.inject.Qualifier");
  private static final ClassName JAKARTA_QUALIFIER_ANNOTATION =
      ClassName.of("jakarta.inject.Qualifier");

  public static final ImmutableSet<ClassName> QUALIFIER_ANNOTATIONS =
      ImmutableSet.of(
          GUICE_BINDING_ANNOTATION, JAVAX_QUALIFIER_ANNOTATION, JAKARTA_QUALIFIER_ANNOTATION);

  public static final Matcher<AnnotationTree> IS_QUALIFIER_ANNOTATION =
      anyOf(
          QUALIFIER_ANNOTATIONS.stream()
              .map(Matchers::symbolHasAnnotation)
              .collect(toImmutableSet()));

  public static final Matcher<ClassTree> HAS_QUALIFIER_ANNOTATION =
      hasAnyAnnotationOf(QUALIFIER_ANNOTATIONS);

  public static final ClassName GUICE_MAP_KEY_ANNOTATION =
      ClassName.of("com.google.inject.multibindings.MapKey");
  public static final ClassName DAGGER_MAP_KEY_ANNOTATION = ClassName.of("dagger.MapKey");

  public static final ClassName DAGGER_COMPONENT_BUILDER_ANNOTATION =
      ClassName.of("dagger.Component.Builder");
  public static final ClassName DAGGER_SUBCOMPONENT_BUILDER_ANNOTATION =
      ClassName.of("dagger.Subcomponent.Builder");
  public static final ClassName DAGGER_PRODUCTION_COMPONENT_BUILDER_ANNOTATION =
      ClassName.of("dagger.producers.ProductionComponent.Builder");
  public static final ClassName DAGGER_PRODUCTION_SUBCOMPONENT_BUILDER_ANNOTATION =
      ClassName.of("dagger.producers.ProductionSubcomponent.Builder");

  public static final Matcher<ClassTree> IS_DAGGER_COMPONENT =
      hasAnyAnnotationOf(
          ImmutableSet.of(
              ClassName.of("dagger.Component"),
              ClassName.of("dagger.Subcomponent"),
              ClassName.of("dagger.producers.ProductionComponent"),
              ClassName.of("dagger.producers.ProductionSubcomponent"),
              ClassName.of("dagger.hilt.DefineComponent")));

  private static final ClassName DAGGER_MODULE = ClassName.of("dagger.Module");

  public static final Matcher<ClassTree> IS_DAGGER_COMPONENT_OR_MODULE =
      anyOf(IS_DAGGER_COMPONENT, hasAnnotation(DAGGER_MODULE));
}
