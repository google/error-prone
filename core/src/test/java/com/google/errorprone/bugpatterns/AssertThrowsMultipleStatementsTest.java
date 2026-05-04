/*
 * Copyright 2018 The Error Prone Authors.
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

/** {@link AssertThrowsMultipleStatements}Test */
@RunWith(JUnit4.class)
public class AssertThrowsMultipleStatementsTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(AssertThrowsMultipleStatements.class, getClass());

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(AssertThrowsMultipleStatements.class, getClass());

  @Test
  public void ignoreInThrowingRunnables() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      System.err.println();
                    });
                IllegalStateException e =
                    assertThrows(
                        IllegalStateException.class,
                        () -> {
                          System.err.println(1);
                          System.err.println(2);
                        });
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      int x = 2;
                      int y = x;
                    });
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      System.err.println();
                    });
                System.err.println(1);
                IllegalStateException e =
                    assertThrows(IllegalStateException.class, () -> System.err.println(2));
                int x = 2;
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      int y = x;
                    });
              }
            }
            """)
        .doTest();
  }

  @Test
  public void complexSingleStatementLambdas() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                assertThrows(IllegalStateException.class, () -> {});
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      System.err.println();
                    });
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      int x = 1;
                    });
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      // BUG: Diagnostic contains:
                      if (true) {
                        System.err.println();
                      }
                    });
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      // BUG: Diagnostic contains:
                      try {
                        System.err.println();
                      } catch (Exception e) {
                      }
                    });
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      // BUG: Diagnostic contains:
                      {
                        System.err.println();
                      }
                    });
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      // BUG: Diagnostic contains:
                      return;
                    });
              }
            }
            """)
        .doTest();
  }
}
