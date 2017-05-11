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
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.inject.Scope;

/** @author sgoldfeder@google.com(Steven Goldfeder) */
public class InvalidTargetingOnScopingAnnotationNegativeCases {

  /** A scoping annotation with no specified target. */
  @Scope
  @Retention(RUNTIME)
  public @interface TestAnnotation1 {}

  /** A scoping annotation that contains more than the required */
  @Target({TYPE, METHOD, PARAMETER})
  @Scope
  @Retention(RUNTIME)
  public @interface TestAnnotation2 {}

  /** A scoping annotation with legal targeting. */
  @Target({TYPE, METHOD})
  @Scope
  @Retention(RUNTIME)
  public @interface TestAnnotation3 {}

  /**
   * A non-scoping annotation with targeting that would be illegal if it were a scoping annotation.
   */
  @Target(PARAMETER)
  @Retention(RUNTIME)
  public @interface TestAnnotation4 {}
}
