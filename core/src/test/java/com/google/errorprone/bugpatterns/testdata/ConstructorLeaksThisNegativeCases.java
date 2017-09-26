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

import com.google.common.util.concurrent.MoreExecutors;
import com.google.errorprone.annotations.Immutable;
import java.util.Objects;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Negative test cases for the ConstructorLeaksThis checker. */
@Immutable
public class ConstructorLeaksThisNegativeCases {
  // Named for com.google.testing.junit.junit4.rules.FixtureController,
  // which is generally initialized with a leaked 'this'.
  private static class FixtureController {
    FixtureController(@SuppressWarnings("unused") Object testObject) {}
  }

  // Instance initializer containing some negative cases
  {
    // 'this' names the Runnable
    MoreExecutors.directExecutor()
        .execute(
            new Runnable() {
              @Override
              public void run() {
                new FixtureController(this);
              }
            });
  }

  public static class SafeReferences {
    static final FixtureController that = new FixtureController(FixtureController.class);

    final FixtureController controller = new FixtureController(that);
    final int hash = Objects.hash(that);
    final String str;

    // Passing out references other than 'this' from a constructor is safe
    public SafeReferences(String str) {
      new FixtureController(str);
      System.out.println(str);
      // 'this' on the LHS is not a leak
      this.str = "Hi";
      // Extracting a field from this, assuming it's initialized,
      // does not constitute a leak
      System.out.println(this.str);
      // This is silly but not unsafe
      System.out.println(SafeReferences.this.str);
    }

    // Exporting 'this' from a regular method is safe
    public void run() {
      new FixtureController(this);
      System.out.println(this);
    }
  }

  // Safe because local variable is not a field
  private void localVariable() {
    int i = java.util.Objects.hashCode(this);
  }

  @RunWith(JUnit4.class)
  public static class JUnitTest {
    // Safe because we skip the check in JUnit tests.
    final int hash = Objects.hash(this);
  }

  public static class ThisIsAnonymous {
    ThisIsAnonymous() {
      // 'this' names not the object under construction, but the Thread
      new Thread() {
        @Override
        public void run() {
          System.out.println(this);
        }
      }.start();
    }
  }

  static class Inner {
    @SuppressWarnings("ConstructorLeaksThis")
    final FixtureController that = new FixtureController(this);
  }
}
