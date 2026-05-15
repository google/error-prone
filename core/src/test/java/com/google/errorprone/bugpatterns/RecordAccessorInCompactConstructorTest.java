/*
 * Copyright 2026 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link RecordAccessorInCompactConstructor}Test */
@RunWith(JUnit4.class)
public final class RecordAccessorInCompactConstructorTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(RecordAccessorInCompactConstructor.class, getClass());

  @Test
  public void accessorCalledInCompactConstructor_flags() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            record Test(int d) {
              public Test {
                // BUG: Diagnostic contains: RecordAccessorInCompactConstructor
                int x = d();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void explicitThisAccessorCalledInCompactConstructor_flags() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            record Test(int d) {
              public Test {
                // BUG: Diagnostic contains: RecordAccessorInCompactConstructor
                int x = this.d();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void accessorCalledInNormalConstructor_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            record Test(int d) {
              public Test(int d) {
                this.d = d;
                int x = d();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void otherInstanceAccessorCalled_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            record Test(int d) {
              public Test {
                Test other = new Test(1);
                int x = other.d();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonAccessorMethodCalled_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            record Test(int d) {
              public Test {
                foo();
              }

              void foo() {}
            }
            """)
        .doTest();
  }

  @Test
  public void parameterAccessedDirectly_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            record Test(int d) {
              public Test {
                int x = d;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void normalClass_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private final int d;

              Test(int d) {
                this.d = d;
                int x = d();
              }

              int d() {
                return d;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void fixApplied() {
    BugCheckerRefactoringTestHelper.newInstance(
            RecordAccessorInCompactConstructor.class, getClass())
        .addInputLines(
            "Test.java",
            """
            record Test(int d) {
              public Test {
                int x = d();
                int y = this.d();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            record Test(int d) {
              public Test {
                int x = d;
                int y = d;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void lambdaInsideCompactConstructor_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            record Test(int d) {
              public Test {
                Runnable r =
                    () -> {
                      int x = d();
                    };
              }
            }
            """)
        .doTest();
  }

  @Test
  public void anonymousClassInsideCompactConstructor_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            record Test(int d) {
              public Test {
                var r =
                    new Runnable() {
                      @Override
                      public void run() {
                        int x = d();
                      }
                    };
              }
            }
            """)
        .doTest();
  }

  @Test
  public void multipleComponents_flags() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            record Test(int a, String b, double c) {
              public Test {
                // BUG: Diagnostic contains: RecordAccessorInCompactConstructor
                int x = a();
                // BUG: Diagnostic contains: RecordAccessorInCompactConstructor
                String y = b();
                // BUG: Diagnostic contains: RecordAccessorInCompactConstructor
                double z = this.c();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void customAccessor_flags() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            record Test(int d) {
              public Test {
                // BUG: Diagnostic contains: RecordAccessorInCompactConstructor
                int x = d();
              }

              @Override
              public int d() {
                return d;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedRecord_flags() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Outer {
              record Inner(int d) {
                public Inner {
                  // BUG: Diagnostic contains: RecordAccessorInCompactConstructor
                  int x = d();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedRecord_callingOuterAccessor_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            record Outer(int d) {
              public Outer {
                int x = d;
              }

              record Inner(int e) {
                public Inner {
                  Outer o = new Outer(1);
                  int x = o.d();
                }
              }
            }
            """)
        .doTest();
  }
}
