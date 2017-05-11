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

/**
 * Positive test cases in which a scoping annotation is put on an interface or anabstract class. The
 * suggested fix is to remove the scoping annotation.
 *
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class ScopeAnnotationOnInterfaceOrAbstractClassPositiveCases {

  /** An abstract class that has scoping annotation. */
  // BUG: Diagnostic contains: remove
  @Singleton
  public abstract class TestClass1 {}

  /** An interface interface has scoping annotation. */
  // BUG: Diagnostic contains: remove
  @Singleton
  public interface TestClass2 {}
}
