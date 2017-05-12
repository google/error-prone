/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.testdata;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** @author scottjohnson@google.com (Scott Johnsson) */
@NonRuntimeAnnotationPositiveCases.NotSpecified
@NonRuntimeAnnotationPositiveCases.NonRuntime
public class NonRuntimeAnnotationPositiveCases {

  public NonRuntime testAnnotation() {
    // BUG: Diagnostic contains:
    NonRuntimeAnnotationPositiveCases.class.getAnnotation(
        NonRuntimeAnnotationPositiveCases.NonRuntime.class);
    // BUG: Diagnostic contains:
    NonRuntimeAnnotationPositiveCases.class.getAnnotation(
        NonRuntimeAnnotationPositiveCases.NotSpecified.class);
    // BUG: Diagnostic contains:
    return this.getClass().getAnnotation(NonRuntimeAnnotationPositiveCases.NonRuntime.class);
  }

  /** Annotation that is explicitly NOT retained at runtime */
  @Retention(RetentionPolicy.SOURCE)
  public @interface NonRuntime {}

  /** Annotation that is implicitly NOT retained at runtime */
  public @interface NotSpecified {}
}
