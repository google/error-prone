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

/** @author sgoldfeder@google.com (Steven Goldfeder) */
public class OverlappingQualifierAndScopeAnnotationNegativeCases {

  @javax.inject.Scope
  @interface MyJavaxScope {}

  @com.google.inject.ScopeAnnotation
  @interface MyGuiceScope {}

  @javax.inject.Qualifier
  @interface MyJavaxQualifier {}

  @com.google.inject.BindingAnnotation
  @interface MyGuiceBindingAnnotation {}

  // supression tests
  @SuppressWarnings("OverlappingQualifierAndScopeAnnotation")
  @javax.inject.Scope
  @javax.inject.Qualifier
  @interface JavaxScopeAndJavaxQualifier {}

  @SuppressWarnings("OverlappingQualifierAndScopeAnnotation")
  @com.google.inject.ScopeAnnotation
  @javax.inject.Qualifier
  @interface GuiceScopeAndJavaxQualifier {}

  @SuppressWarnings("OverlappingQualifierAndScopeAnnotation")
  @com.google.inject.ScopeAnnotation
  @com.google.inject.BindingAnnotation
  @interface GuiceScopeAndGuiceBindingAnnotation {}

  @SuppressWarnings("OverlappingQualifierAndScopeAnnotation")
  @javax.inject.Scope
  @com.google.inject.BindingAnnotation
  @interface JavaxScopeAndGuiceBindingAnnotation {}
}
