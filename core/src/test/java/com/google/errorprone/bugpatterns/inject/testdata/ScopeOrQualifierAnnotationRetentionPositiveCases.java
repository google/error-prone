/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.inject.testdata;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import com.google.inject.BindingAnnotation;
import com.google.inject.ScopeAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.inject.Qualifier;
import javax.inject.Scope;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
public class ScopeOrQualifierAnnotationRetentionPositiveCases {
  /** A scoping (@Scope) annotation with SOURCE retention */
  @Scope
  @Target({TYPE, METHOD})
  // BUG: Diagnostic contains: @Retention(RUNTIME)
  @Retention(SOURCE)
  public @interface TestAnnotation1 {}

  /** A scoping (@ScopingAnnotation) annotation with SOURCE retention. */
  @ScopeAnnotation
  @Target({TYPE, METHOD})
  // BUG: Diagnostic contains: @Retention(RUNTIME)
  @Retention(SOURCE)
  public @interface TestAnnotation2 {}

  /** A qualifer(@Qualifier) annotation with SOURCE retention. */
  @Qualifier
  @Target({TYPE, METHOD})
  // BUG: Diagnostic contains: @Retention(RUNTIME)
  @Retention(SOURCE)
  public @interface TestAnnotation3 {}

  /** A qualifer(@BindingAnnotation) annotation with SOURCE retention. */
  @BindingAnnotation
  @Target({TYPE, METHOD})
  // BUG: Diagnostic contains: @Retention(RUNTIME)
  @Retention(SOURCE)
  public @interface TestAnnotation4 {}

  /** A qualifer annotation with default retention. */
  @BindingAnnotation
  @Target({TYPE, METHOD})
  // BUG: Diagnostic contains: @Retention(RUNTIME)
  public @interface TestAnnotation5 {}
}
