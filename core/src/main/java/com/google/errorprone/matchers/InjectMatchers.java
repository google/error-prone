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

package com.google.errorprone.matchers;

import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.Tree;

/** Utility constants and matchers related to dependency injection. */
public final class InjectMatchers {
  private InjectMatchers() {} // no instantiation

  public static final String GUICE_PROVIDES_ANNOTATION = "com.google.inject.Provides";

  public static final String ASSISTED_ANNOTATION = "com.google.inject.assistedinject.Assisted";
  public static final String ASSISTED_INJECT_ANNOTATION =
      "com.google.inject.assistedinject.AssistedInject";

  public static final String GUICE_INJECT_ANNOTATION = "com.google.inject.Inject";
  public static final String JAVAX_INJECT_ANNOTATION = "javax.inject.Inject";

  public static final String GUICE_SCOPE_ANNOTATION = "com.google.inject.ScopeAnnotation";
  public static final String JAVAX_SCOPE_ANNOTATION = "javax.inject.Scope";

  public static final String GUICE_BINDING_ANNOTATION = "com.google.inject.BindingAnnotation";
  public static final String JAVAX_QUALIFIER_ANNOTATION = "javax.inject.Qualifier";

  public static final Matcher<AnnotationTree> IS_APPLICATION_OF_JAVAX_INJECT =
      new AnnotationType(JAVAX_INJECT_ANNOTATION);

  private static final Matcher<Tree> HAS_INJECT_ANNOTATION_MATCHER =
      anyOf(hasAnnotation(GUICE_INJECT_ANNOTATION), hasAnnotation(JAVAX_INJECT_ANNOTATION));

  @SuppressWarnings("unchecked") // Safe contravariant cast
  public static <T extends Tree> Matcher<T> hasInjectAnnotation() {
    return (Matcher<T>) HAS_INJECT_ANNOTATION_MATCHER;
  }

  private static final Matcher<Tree> DAGGER_COMPONENT_MATCHER =
      anyOf(hasAnnotation("dagger.Component"), hasAnnotation("dagger.Subcomponent"));

  @SuppressWarnings("unchecked") // Safe contravariant cast
  public static <T extends Tree> Matcher<T> isDaggerComponent() {
    return (Matcher<T>) DAGGER_COMPONENT_MATCHER;
  }

  public static MultiMatcher<Tree, AnnotationTree> hasScopingAnnotations() {
    return annotations(
        AT_LEAST_ONE,
        Matchers.<AnnotationTree>anyOf(
            hasAnnotation(GUICE_SCOPE_ANNOTATION), hasAnnotation(JAVAX_SCOPE_ANNOTATION)));
  }
}
