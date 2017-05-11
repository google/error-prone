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

import com.google.inject.assistedinject.AssistedInject;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
public class AssistedInjectAndInjectOnSameConstructorPositiveCases {
  /** Class has a constructor annotated with @javax.inject.Inject and @AssistedInject. */
  public class TestClass1 {
    // BUG: Diagnostic contains: remove
    @javax.inject.Inject
    // BUG: Diagnostic contains: remove
    @AssistedInject
    public TestClass1() {}
  }

  /** Class has a constructor annotated with @com.google.inject.Inject and @AssistedInject. */
  public class TestClass2 {
    // BUG: Diagnostic contains: remove
    @com.google.inject.Inject
    // BUG: Diagnostic contains: remove
    @AssistedInject
    public TestClass2() {}
  }
}
