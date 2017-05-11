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

import com.google.inject.Singleton;
import com.google.inject.servlet.SessionScoped;
import javax.inject.Scope;

/** @author sgoldfeder@google.com(Steven Goldfeder) */
public class MoreThanOneScopeAnnotationOnClassPositiveCases {

  /** Class has two scope annotations */
  @Singleton
  @SessionScoped
  // BUG: Diagnostic contains: @Singleton(), @SessionScoped().
  class TestClass1 {}

  /** Class has three annotations, two of which are scope annotations. */
  @Singleton
  @SuppressWarnings("foo")
  @SessionScoped
  // BUG: Diagnostic contains: @Singleton(), @SessionScoped().
  class TestClass2 {}

  @Scope
  @interface CustomScope {}

  @Singleton
  @CustomScope
  @SessionScoped
  // BUG: Diagnostic contains: @Singleton(), @CustomScope(), @SessionScoped().
  class TestClass3 {}
}
