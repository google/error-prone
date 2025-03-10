/*
 * Copyright 2024 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public final class MisformattedTestDataTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MisformattedTestData.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(MisformattedTestData.class, getClass());

  @Test
  public void alreadyFormatted_noFinding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;

            class Test {
              void method(BugCheckerRefactoringTestHelper h) {
                h.addInputLines(
                    "Test.java",
                    \"""
                    package foo;

                    class Test {
                      void method() {
                        int a = 1;
                      }
                    }
                    \""");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void onlyDiffersByFinalNewline_noFinding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;

            class Test {
              void method(BugCheckerRefactoringTestHelper h) {
                h.addInputLines(
                    "Test.java",
                    \"""
                    package foo;

                    class Test {
                      void method() {
                        int a = 1;
                      }
                    }\""");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void misformatted_suggestsFix() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;

            class Test {
              void method(BugCheckerRefactoringTestHelper h) {
                h.addInputLines(
                        "Test.java",
                        \"""
                        package foo;
                        class Test {
                          void method() {
                            int a =
                            1;
                          }
                        }
                        \""")
                    .addOutputLines(
                        "Test.java",
                        \"""
                        package foo;
                        class Test {
                          void method() {
                            int a =
                            1;
                          }
                        }
                        \""");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;

            class Test {
              void method(BugCheckerRefactoringTestHelper h) {
                h.addInputLines(
                        "Test.java",
                        \"""
                        package foo;

                        class Test {
                          void method() {
                            int a = 1;
                          }
                        }
                        \""")
                  .addOutputLines(
                        "Test.java",
                        \"""
                        package foo;

                        class Test {
                          void method() {
                            int a = 1;
                          }
                        }
                        \""");
               }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void trailingComments_notIncludedInPrefix() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;

            class Test {
              void method(BugCheckerRefactoringTestHelper h) {
                h.addInputLines(
                    "Test.java", //
                    "package foo; class Test {}");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;

            class Test {
              void method(BugCheckerRefactoringTestHelper h) {
                h.addInputLines(
                    "Test.java", //
                    \"""
                    package foo;

                    class Test {}
                    \""");
               }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void onlyDiffersByIndentation_notReindented() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;

            class Test {
              void method(BugCheckerRefactoringTestHelper h) {
                h.addInputLines(
                    "Test.java",
                    \"""
                                            package foo;

                                            class Test {
                                              void method() {
                                                int a = 1;
                                              }
                                            }
                                            \""");
              }
            }
            """)
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void escapesSpecialCharacters() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;

            class Test {
              void method(BugCheckerRefactoringTestHelper h) {
                h.addInputLines(
                    "Test.java",
                    \"""
                    package foo;

                    class Test {
                      void method() {
                        var foo
                            = "foo\\\\nbar";
                      }
                    }
                    \""");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.BugCheckerRefactoringTestHelper;

            class Test {
              void method(BugCheckerRefactoringTestHelper h) {
                h.addInputLines(
                    "Test.java",
                    \"""
                    package foo;

                    class Test {
                      void method() {
                        var foo = "foo\\\\nbar";
                      }
                    }
                    \""");
               }
            }
            """)
        .doTest(TEXT_MATCH);
  }
}
