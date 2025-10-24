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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link MissingDefault}Test */
@RunWith(JUnit4.class)
public class MissingDefaultTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MissingDefault.class, getClass());

  @Test
  public void positive() {
    BugCheckerRefactoringTestHelper.newInstance(MissingDefault.class, getClass())
        .addInputLines(
            "in/Test.java",
            """
            class Test {
              boolean f(int i) {
                // BUG: Diagnostic contains:
                switch (i) {
                  case 42:
                    return true;
                }
                return false;
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            class Test {
              boolean f(int i) {
                // BUG: Diagnostic contains:
                switch (i) {
                  case 42:
                    return true;
                  default: // fall out
                }
                return false;
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void positiveBreak() {
    BugCheckerRefactoringTestHelper.newInstance(MissingDefault.class, getClass())
        .addInputLines(
            "in/Test.java",
            """
            class Test {
              void f(int i) {
                // BUG: Diagnostic contains:
                switch (i) {
                  case 42:
                    System.err.println(42);
                }
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            class Test {
              void f(int i) {
                // BUG: Diagnostic contains:
                switch (i) {
                  case 42:
                    System.err.println(42);
                    break;
                  default: // fall out
                }
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(int i) {
                switch (i) {
                  case 42:
                    return true;
                  default:
                    return false;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void enumSwitch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum E {
                ONE,
                TWO
              }

              boolean f(E e) {
                switch (e) {
                  case ONE:
                    return true;
                }
                return false;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void empty() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(int i) {
                switch (i) {
                  case 42:
                    return true;
                  default: // fall out
                }
                return false;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void emptyNoComment() {
    BugCheckerRefactoringTestHelper.newInstance(MissingDefault.class, getClass())
        .addInputLines(
            "in/Test.java",
            """
            class Test {
              boolean f(int i) {
                switch (i) {
                  case 42:
                    return true;
                  default:
                }
                return false;
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            class Test {
              boolean f(int i) {
                switch (i) {
                  case 42:
                    return true;
                  default: // fall out
                }
                return false;
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void interiorEmptyNoComment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(int i) {
                switch (i) {
                  default:
                  case 42:
                    return true;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void multipleStatementsInGroup() {
    BugCheckerRefactoringTestHelper.newInstance(MissingDefault.class, getClass())
        .addInputLines(
            "in/Test.java",
            """
            class Test {
              boolean f(int i) {
                // BUG: Diagnostic contains:
                switch (i) {
                  case 42:
                    System.err.println();
                    return true;
                }
                return false;
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            class Test {
              boolean f(int i) {
                // BUG: Diagnostic contains:
                switch (i) {
                  case 42:
                    System.err.println();
                    return true;
                  default: // fall out
                }
                return false;
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void arrowSwitch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m(int i) {
                // BUG: Diagnostic contains:
                switch (i) {
                  case 1 -> {}
                  case 2 -> {}
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arrowSwitchNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m(int i) {
                switch (i) {
                  case 1 -> {}
                  case 2 -> {}
                  default -> {} // fall out
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arrowComment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f() {}

              void m(int i) {
                switch (i) {
                  case 0 -> f();
                  case 1 -> f();
                  default -> f();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void exhaustiveExpressionSwitch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              sealed interface I permits A, B {}

              abstract static class S {}

              static final class A extends S implements I {}

              static final class B extends S implements I {}

              void f(I i) {
                switch (i) {
                  case A a -> System.err.println(a);
                  case B b -> System.err.println(b);
                }
                ;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arrowSwitchPositive() {
    BugCheckerRefactoringTestHelper.newInstance(MissingDefault.class, getClass())
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f(int i) {
                switch (i) {
                  case 0 -> System.err.println(i);
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f(int i) {
                switch (i) {
                  case 0 -> System.err.println(i);
                  default -> {}
                }
              }
            }
            """)
        .doTest();
  }
}
