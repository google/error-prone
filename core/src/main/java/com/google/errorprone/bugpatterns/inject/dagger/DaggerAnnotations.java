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
package com.google.errorprone.bugpatterns.inject.dagger;

import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;

import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.Tree;

/** A utility class for static analysis having to do with Dagger annotations. */
public final class DaggerAnnotations {

  // Dagger types
  static final String BINDS_CLASS_NAME = "dagger.Binds";
  static final String PROVIDES_CLASS_NAME = "dagger.Provides";
  static final String MODULE_CLASS_NAME = "dagger.Module";
  static final String MULTIBINDS_CLASS_NAME = "dagger.multibindings.Multibinds";

  // Dagger Producers types
  static final String PRODUCES_CLASS_NAME = "dagger.producers.Produces";
  static final String PRODUCER_MODULE_CLASS_NAME = "dagger.producers.ProducerModule";

  // Multibinding types
  static final String INTO_SET_CLASS_NAME = "dagger.multibindings.IntoSet";
  static final String ELEMENTS_INTO_SET_CLASS_NAME = "dagger.multibindings.ElementsIntoSet";
  static final String INTO_MAP_CLASS_NAME = "dagger.multibindings.IntoMap";

  private static final Matcher<Tree> ANY_MODULE =
      anyOf(hasAnnotation(MODULE_CLASS_NAME), hasAnnotation(PRODUCER_MODULE_CLASS_NAME));

  private static final Matcher<Tree> BINDING_METHOD =
      anyOf(hasAnnotation(PROVIDES_CLASS_NAME), hasAnnotation(PRODUCES_CLASS_NAME));

  private static final Matcher<Tree> BINDING_DECLARATION_METHOD =
      anyOf(hasAnnotation(BINDS_CLASS_NAME), hasAnnotation(MULTIBINDS_CLASS_NAME));

  // Common Matchers
  public static Matcher<Tree> isAnyModule() {
    return ANY_MODULE;
  }

  static Matcher<Tree> isBindingMethod() {
    return BINDING_METHOD;
  }

  static Matcher<Tree> isBindingDeclarationMethod() {
    return BINDING_DECLARATION_METHOD;
  }

  private DaggerAnnotations() {}
}
