/*
 * Copyright 2017 The Error Prone Authors.
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

import com.google.errorprone.annotations.Immutable;

/** Positive test cases for {@link ConstructorInvokesOverridable}. */
@Immutable
public class ConstructorInvokesOverridablePositiveCases {

  // BUG: Diagnostic contains: Constructors should not invoke overridable
  final int i = unsafe();

  {
    // BUG: Diagnostic contains: Constructors should not invoke overridable
    unsafe();
  }

  ConstructorInvokesOverridablePositiveCases() {
    // BUG: Diagnostic contains: Constructors should not invoke overridable
    unsafe();
    // BUG: Diagnostic contains: Constructors should not invoke overridable
    this.unsafe();
    // BUG: Diagnostic contains: Constructors should not invoke overridable
    ConstructorInvokesOverridablePositiveCases.this.unsafe();

    new Thread() {
      @Override
      public void run() {
        // BUG: Diagnostic contains: Constructors should not invoke overridable
        unsafe();
      }
    }.start();

    // BUG: Diagnostic contains: Constructors should not invoke overridable
    new Thread(() -> unsafe()).start();
  }

  protected int unsafe() {
    return 0;
  }

  void localInitializer() {
    class Local extends java.util.HashMap<String, String> {
      {
        // BUG: Diagnostic contains: Constructors should not invoke overridable
        put("Hi", "Mom");
      }
    }
  }

  // Lookup is handled correctly for inner classes as well
  class Inner {
    // BUG: Diagnostic contains: Constructors should not invoke overridable
    final int unsafeValue = innerUnsafe();

    protected int innerUnsafe() {
      return 7;
    }
  }
}
