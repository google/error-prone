/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Positive test cases for {@link ConstructorLeaksThis}. */
@Immutable
public class ConstructorLeaksThisPositiveCases {
  // Named for com.google.testing.junit.junit4.rules.FixtureController,
  // which is generally initialized with a leaked 'this'.
  private static class FixtureController {
    FixtureController(@SuppressWarnings("unused") Object testObject) {}
  }

  // Method invocation in field initializer
  // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
  final int hash = Objects.hash(this);

  // New call in field initializer
  // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
  final FixtureController controller = new FixtureController(this);

  {
    // Method invocation in instance initializer
    // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
    System.out.println(this);

    // New call in instance initializer
    // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
    new FixtureController(this);
  }

  ConstructorLeaksThisPositiveCases() {
    // Method invocation in constructor
    // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
    System.out.println(this);

    // New call in constructor
    // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
    new AtomicReference<>(this);

    // Qualified reference inside a context where plain 'this' means something else
    new Thread() {
      @Override
      public void run() {
        // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
        System.out.println(ConstructorLeaksThisPositiveCases.this);
      }
    }.start();
    Runnable r =
        () ->
            System.out.println(
                // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
                com.google.errorprone.bugpatterns.testdata.ConstructorLeaksThisPositiveCases.this);
    // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
    r = () -> System.out.println(this);
  }

  public static class ThisInLambda {
    {
      // 'this' names not the lambda but the class under construction
      // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
      new Thread(() -> System.out.println(this)).start();
    }
  }

  /** Leak is signaled even if masked by cast expression */
  static class CastRunnable implements Runnable {
    CastRunnable() {
      // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
      new Thread((Runnable) this).start();
    }

    @Override
    public void run() {}
  }
}
