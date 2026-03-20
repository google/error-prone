/*
 * Copyright 2023 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class NotJavadocTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(NotJavadoc.class, getClass());
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(NotJavadoc.class, getClass());

  @Test
  public void notJavadoc() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test() {
                /** Not Javadoc. */
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void test() {
                /* Not Javadoc. */
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void nestedClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void test() {
                // BUG: Diagnostic contains: local class
                /** Not Javadoc. */
                class A {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedClassWithMethod() {
    // TODO(kak): we should also fix the "javadocs" on the method inside the local class
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void test() {
                class A {
                  /** Not Javadoc. */
                  void method() {}
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void doubleJavadoc() {
    // It would be nice if this were caught.
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              /** Not Javadoc. */
              /** Javadoc. */
              void test() {}
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void notJavadocOnLocalClass() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test() {
                /** Not Javadoc. */
                class A {
                  /** Not Javadoc. */
                  void method() {}
                }
              }
            }
            """)
        // TODO(kak): we should also fix the "javadocs" on the method inside the local class
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void test() {
                /* Not Javadoc. */
                class A {
                  /** Not Javadoc. */
                  void method() {}
                }
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void notJavadocWithLotsOfAsterisks() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test() {
                /******** Not Javadoc. */
                class A {}
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void test() {
                /* Not Javadoc. */
                class A {}
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void actuallyJavadoc() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              /** Not Javadoc. */
              void test() {}
            }
            """)
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void strangeComment() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test() {
                /**/
              }
            }
            """)
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void packageLevel() {
    helper
        .addInputLines(
            "package-info.java",
            """
            /** Package javadoc */
            package foo;
            """)
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void moduleLevel() {
    helper
        .addInputLines(
            "module-info.java",
            """
            /** Module javadoc */
            module foo {}
            """)
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suppression() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              @SuppressWarnings("NotJavadoc")
              void test() {
                /** Not Javadoc. */
              }
            }
            """)
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void recordComponentWithClassicJavadoc() {
    helper
        .addInputLines(
            "Test.java",
            """
            public record Test(
                /** age (must be positive) */
                int age) {}
            """)
        .addOutputLines(
            "Test.java",
            """
            public record Test(
                /* age (must be positive) */
                int age) {}
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void recordComponentWithMultiLineClassicJavadoc() {
    helper
        .addInputLines(
            "Test.java",
"""
public record Test(
    /**
     * @param age Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor
     *     incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
     *     exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure
     *     dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
     *     Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit
     *     anim id est laborum.
     */
    int age) {}
""")
        // TODO(kak): it would be nice to hoist the @param up to the record's Javadocs
        .addOutputLines(
            "Test.java",
"""
public record Test(
    /*
     * @param age Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor
     *     incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
     *     exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure
     *     dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
     *     Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit
     *     anim id est laborum.
     */
    int age) {}
""")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void recordComponentWithMarkdownJavadoc() {
    helper
        .addInputLines(
            "Test.java",
            """
            public record Test(
                /// age (must be positive)
                int age) {}
            """)
        // TODO(b/494275366): Add a fix for this.
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void recordComponentWithMultiLineMarkdownJavadoc() {
    helper
        .addInputLines(
            "Test.java",
            """
            public record Test(
                /// @param age Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
                ///     eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad
                ///     minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
                ///     ex ea commodo consequat. Duis aute irure dolor in reprehenderit in
                ///     voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur
                ///     sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
                ///     mollit anim id est laborum.
                int age) {}
            """)
        // TODO(b/494275366): Add a fix for this.
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }
}
