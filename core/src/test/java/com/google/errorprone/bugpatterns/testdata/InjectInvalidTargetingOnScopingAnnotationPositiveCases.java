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

package com.google.errorprone.bugpatterns;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Target;

import javax.inject.Scope;

/**
 * @author sgoldfeder@google.com(Steven Goldfeder)
 */
public class InjectInvalidTargetingOnScopingAnnotationPositiveCases {

  /**
   * A scoping annotation with no specified target.
   */
  @Scope 
  // BUG: Diagnostic contains: @Target({TYPE, METHOD})
  public @interface TestAnnotation1 {
  }

  /**
   * @Target is given an empty array
   */
  // BUG: Diagnostic contains: @Target({TYPE, METHOD})
  @Target({})
  @Scope 
  public @interface TestAnnotation2 {
  }

  /**
   * A scoping annotation with taeget TYPE, METHOD, and (illegal) PARAMETER.
   */
  // BUG: Diagnostic contains: @Target({TYPE, METHOD})
  @Target({TYPE, METHOD, PARAMETER})
  @Scope 
  public @interface TestAnnotation3 {
  }

  /**
   * A scoping annotation target set to PARAMETER.
   */
  // BUG: Diagnostic contains: @Target({TYPE, METHOD})
  @Target(PARAMETER)
  @Scope 
  public @interface TestAnnotation4 {
  }
}
