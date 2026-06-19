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

package com.google.errorprone.bugpatterns.nullness;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link RequireNonNullRefactoring}. */
@RunWith(JUnit4.class)
public final class RequireNonNullRefactoringTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(RequireNonNullRefactoring.class, getClass());

  @Test
  public void refactoringNoMessage() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void foo(Object bar) {
                if (bar == null) {
                  throw new NullPointerException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static java.util.Objects.requireNonNull;

            class Test {
              void foo(Object bar) {
                requireNonNull(bar);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void refactoringConstantMessage() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void foo(Object bar) {
                if (null == bar) {
                  throw new NullPointerException("bar must not be null");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static java.util.Objects.requireNonNull;

            class Test {
              void foo(Object bar) {
                requireNonNull(bar, "bar must not be null");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void noBraces() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void foo(Object bar) {
                if (bar == null) throw new NullPointerException();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static java.util.Objects.requireNonNull;

            class Test {
              void foo(Object bar) {
                requireNonNull(bar);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeWrongException() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void foo(Object bar) {
                if (bar == null) {
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeWrongCondition() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void foo(Object bar) {
                if (bar != null) {
                  throw new NullPointerException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeMultipleStatements() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void foo(Object bar) {
                if (bar == null) {
                  System.out.println("bar is null!");
                  throw new NullPointerException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeNonConstantMessage() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void foo(Object bar) {
                String msg = "bar is null";
                if (bar == null) {
                  throw new NullPointerException(msg);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeHasElse() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void foo(Object bar) {
                if (bar == null) {
                  throw new NullPointerException();
                } else {
                  System.out.println("ok");
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
