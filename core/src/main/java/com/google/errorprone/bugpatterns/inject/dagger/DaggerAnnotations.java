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
package com.google.errorprone.bugpatterns.inject.dagger;

import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;

import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.Tree;

/** A utility class for static analysis having to do with Dagger annotations. */
final class DaggerAnnotations {
  // Dagger types
  static final String BINDS_CLASS_NAME = "dagger.Binds";
  static final String PROVIDES_CLASS_NAME = "dagger.Provides";
  static final String MODULE_CLASS_NAME = "dagger.Module";
  static final String MULTIBINDS_CLASS_NAME = "dagger.multibindings.Multibinds";

  // Dagger matchers
  static <T extends Tree> Matcher<T> isModule() {
    return hasAnnotation(MODULE_CLASS_NAME);
  }

  static <T extends Tree> Matcher<T> isProvidesMethod() {
    return hasAnnotation(PROVIDES_CLASS_NAME);
  }

  static <T extends Tree> Matcher<T> isBindsMethod() {
    return hasAnnotation(BINDS_CLASS_NAME);
  }

  static <T extends Tree> Matcher<T> isMultibindsMethod() {
    return hasAnnotation(MULTIBINDS_CLASS_NAME);
  }

  // Dagger Producers types
  static final String PRODUCES_CLASS_NAME = "dagger.producers.Produces";
  static final String PRODUCER_MODULE_CLASS_NAME = "dagger.producers.ProducerModule";

  // Dagger Producers matchers
  static <T extends Tree> Matcher<T> isProducerModule() {
    return hasAnnotation(PRODUCER_MODULE_CLASS_NAME);
  }

  static <T extends Tree> Matcher<T> isProducesMethod() {
    return hasAnnotation(PRODUCES_CLASS_NAME);
  }

  // Multibinding types
  static final String INTO_SET_CLASS_NAME = "dagger.multibindings.IntoSet";
  static final String ELEMENTS_INTO_SET_CLASS_NAME = "dagger.multibindings.ElementsIntoSet";
  static final String INTO_MAP_CLASS_NAME = "dagger.multibindings.IntoMap";

  static <T extends Tree> Matcher<T> isMultibindingMethod() {
    return anyOf(
        hasAnnotation(INTO_SET_CLASS_NAME),
        hasAnnotation(ELEMENTS_INTO_SET_CLASS_NAME),
        hasAnnotation(INTO_MAP_CLASS_NAME));
  }

  // Common Matchers
  static <T extends Tree> Matcher<T> isAnyModule() {
    return anyOf(isModule(), isProducerModule());
  }

  static <T extends Tree> Matcher<T> isBindingMethod() {
    return anyOf(isProvidesMethod(), isProducesMethod());
  }

  static <T extends Tree> Matcher<T> isBindingDeclarationMethod() {
    return anyOf(isBindsMethod(), isMultibindsMethod());
  }

  static <T extends Tree> Matcher<T> isAnyBindingDeclaringMethod() {
    return anyOf(isBindingMethod(), isBindingDeclarationMethod());
  }

  private DaggerAnnotations() {}
}
