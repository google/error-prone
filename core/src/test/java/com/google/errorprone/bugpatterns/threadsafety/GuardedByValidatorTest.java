/*
 * Copyright 2014 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link GuardedByChecker} annotation syntax GuardedByValidationResult test */
@RunWith(JUnit4.class)
public class GuardedByValidatorTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(GuardedByChecker.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety.Test;

            import com.google.errorprone.annotations.concurrent.GuardedBy;

            class Test {
              @GuardedBy("This thread")
              // BUG: Diagnostic contains:
              // Invalid @GuardedBy expression: could not resolve guard
              int x;

              @GuardedBy("This thread")
              // BUG: Diagnostic contains:
              // Invalid @GuardedBy expression: could not resolve guard
              void m() {}
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety.Test;

            import com.google.errorprone.annotations.concurrent.GuardedBy;

            class Test {
              final Object mu = new Object();

              class Inner {
                @GuardedBy("this")
                int x;

                @GuardedBy("Test.this")
                int p;

                @GuardedBy("Test.this.mu")
                int z;

                @GuardedBy("this")
                void m() {}

                @GuardedBy("mu")
                int v;

                @GuardedBy("itself")
                Object s_;
              }

              final Object o =
                  new Object() {
                    @GuardedBy("mu")
                    int x;
                  };
            }
            """)
        .doTest();
  }

  @Test
  public void itself() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety.Test;

            import com.google.errorprone.annotations.concurrent.GuardedBy;

            class Test {
              @GuardedBy("itself")
              Object s_;
            }
            """)
        .doTest();
  }

  @Test
  public void badInstanceAccess() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety.Test;

            import com.google.errorprone.annotations.concurrent.GuardedBy;

            class Test {
              final Object instanceField = new Object();

              @GuardedBy("Test.instanceField")
              // BUG: Diagnostic contains:
              // Invalid @GuardedBy expression: could not resolve guard
              Object s_;
            }
            """)
        .doTest();
  }

  @Test
  public void className() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety.Test;

            import com.google.errorprone.annotations.concurrent.GuardedBy;

            class Test {
              @GuardedBy("Test")
              // BUG: Diagnostic contains:
              // Invalid @GuardedBy expression: could not resolve guard
              Object s_;
            }
            """)
        .doTest();
  }

  @Test
  public void anonymousClassTypo() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety;

            import com.google.errorprone.annotations.concurrent.GuardedBy;

            class Test {
              static class Endpoint {
                Object getLock() {
                  return null;
                }
              }

              abstract static class Runnable {
                private Endpoint endpoint;

                Runnable(Endpoint endpoint) {
                  this.endpoint = endpoint;
                }

                abstract void run();
              }

              static void m(Endpoint endpoint) {
                Runnable runnable =
                    new Runnable(endpoint) {
                      @GuardedBy("endpoint_.getLock()")
                      // BUG: Diagnostic contains:
                      // Invalid @GuardedBy expression: could not resolve guard
                      void run() {}
                    };
              }
            }
            """)
        .doTest();
  }

  @Test
  public void anonymousClassPrivateAccess() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety;

            import com.google.errorprone.annotations.concurrent.GuardedBy;

            class Test {
              static class Endpoint {
                Object getLock() {
                  return null;
                }
              }

              abstract static class Runnable {
                private Endpoint endpoint;

                Runnable(Endpoint endpoint) {
                  this.endpoint = endpoint;
                }

                abstract void run();
              }

              static void m(Endpoint endpoint) {
                Runnable runnable =
                    new Runnable(endpoint) {
                      @GuardedBy("endpoint.getLock()")
                      // BUG: Diagnostic contains:
                      // Invalid @GuardedBy expression: could not resolve guard
                      void run() {}
                    };
              }
            }
            """)
        .doTest();
  }

  @Test
  public void staticGuardedByInstance() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety.Test;

            import com.google.errorprone.annotations.concurrent.GuardedBy;

            class Test {
              @GuardedBy("this")
              // BUG: Diagnostic contains:
              // Invalid @GuardedBy expression: static member guarded by instance
              static int x;
            }
            """)
        .doTest();
  }

  @Test
  public void staticGuardedByInstanceMethod() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety.Test;

            import com.google.errorprone.annotations.concurrent.GuardedBy;

            class Test {
              final Object mu_ = new Object();

              Object lock() {
                return mu_;
              }

              @GuardedBy("lock()")
              // BUG: Diagnostic contains:
              // Invalid @GuardedBy expression: static member guarded by instance
              static int x;
            }
            """)
        .doTest();
  }

  @Test
  public void staticGuardedByStatic() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety.Test;

            import com.google.errorprone.annotations.concurrent.GuardedBy;

            class Test {
              @GuardedBy("Test.class")
              static int x;
            }
            """)
        .doTest();
  }

  @Test
  public void nonExistantMethod() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety.Test;

            import com.google.errorprone.annotations.concurrent.GuardedBy;

            class Test {
              @GuardedBy("lock()")
              // BUG: Diagnostic contains:
              // Invalid @GuardedBy expression: could not resolve guard
              int x;

              @GuardedBy("lock()")
              // BUG: Diagnostic contains:
              // Invalid @GuardedBy expression: could not resolve guard
              void m() {}
            }
            """)
        .doTest();
  }
}
