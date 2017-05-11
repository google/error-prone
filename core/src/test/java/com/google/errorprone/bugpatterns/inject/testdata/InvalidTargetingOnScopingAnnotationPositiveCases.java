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

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.ScopeAnnotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.inject.Scope;

/** @author sgoldfeder@google.com(Steven Goldfeder) */
public class InvalidTargetingOnScopingAnnotationPositiveCases {

  /** Scoping excludes METHOD */
  // BUG: Diagnostic contains: @Target({TYPE, METHOD})
  @Target(TYPE)
  @Scope
  @Retention(RUNTIME)
  public @interface TestAnnotation1 {}

  /** Scoping excludes TYPE */
  // BUG: Diagnostic contains: @Target({TYPE, METHOD})
  @Target(METHOD)
  @Scope
  @Retention(RUNTIME)
  public @interface TestAnnotation2 {}

  /** Scoping excludes both, but has other elements to preserve */
  // BUG: Diagnostic contains: @Target({TYPE, METHOD, PARAMETER})
  @Target(PARAMETER)
  @Scope
  @Retention(RUNTIME)
  public @interface TestAnnotation4 {}

  /** Scoping includes one of the required ones. */
  // BUG: Diagnostic contains: @Target({TYPE, METHOD, PARAMETER, CONSTRUCTOR})
  @Target({PARAMETER, METHOD, CONSTRUCTOR})
  @Scope
  @Retention(RUNTIME)
  public @interface TestAnnotation5 {}

  /** Same as above, but with a different physical manifestation */
  // BUG: Diagnostic contains: @Target({TYPE, METHOD, PARAMETER, CONSTRUCTOR})
  @Target(value = {ElementType.PARAMETER, METHOD, CONSTRUCTOR})
  @Scope
  @Retention(RUNTIME)
  public @interface TestAnnotation6 {}

  /** Target annotation is empty, nonsensical since it can't be applied to anything */
  // BUG: Diagnostic contains: @Target({TYPE, METHOD})
  @Target({})
  @ScopeAnnotation
  @Retention(RUNTIME)
  public @interface TestAnnotation7 {}
}
