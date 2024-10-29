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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DuplicateBranchesTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(DuplicateBranches.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              String f(boolean a, String b, String c) {
                // BUG: Diagnostic contains:
                return a ? b : b;
              }

              String g(boolean a, String b, String c) {
                // BUG: Diagnostic contains:
                if (a) {
                  return b;
                } else {
                  return b;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              String f(boolean a, String b, String c) {
                return a ? b : c;
              }

              String g(boolean a, String b, String c) {
                if (a) {
                  return b;
                } else {
                  return c;
                }
              }

              String h(boolean a, String b, String c) {
                if (a) {
                  return b;
                }
                return "";
              }
            }
            """)
        .doTest();
  }

  @Test
  public void statementRefactoring() {
    BugCheckerRefactoringTestHelper.newInstance(DuplicateBranches.class, getClass())
        .addInputLines(
            "Test.java",
            """
            class Test {
              String g(boolean a, String b, String c) {
                if (a) {
                  return b;
                } else {
                  return b;
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              String g(boolean a, String b, String c) {
                return b;
              }
            }
            """)
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void statementRefactoringChain() {
    BugCheckerRefactoringTestHelper.newInstance(DuplicateBranches.class, getClass())
        .addInputLines(
            "Test.java",
            """
            class Test {
              String g(boolean a, String b, String c) {
                if (a) {
                  return c;
                } else if (a) {
                  return b;
                } else {
                  return b;
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              String g(boolean a, String b, String c) {
                if (a) {
                  return c;
                } else {
                  return b;
                }
              }
            }
            """)
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void commentRefactoring() {
    BugCheckerRefactoringTestHelper.newInstance(DuplicateBranches.class, getClass())
        .addInputLines(
            "Test.java",
            """
            class Test {
              String g(boolean a, String b, String c) {
                if (a) {
                  // foo
                  return b;
                } else {
                  // bar
                  return b;
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              String g(boolean a, String b, String c) {
                // foo
                // bar
                return b;
              }
            }
            """)
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void commentRefactoringIfElse() {
    BugCheckerRefactoringTestHelper.newInstance(DuplicateBranches.class, getClass())
        .addInputLines(
            "Test.java",
            """
            class Test {
              boolean g(boolean a, boolean b) {
                if (a) {
                  return true;
                } else if (a) {
                  // foo
                  return b;
                } else {
                  // bar
                  return b;
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              boolean g(boolean a, boolean b) {
                if (a) {
                  return true;
                } else {
                  // foo
                  // bar
                  return b;
                }
              }
            }
            """)
        .doTest(TestMode.TEXT_MATCH);
  }
}
