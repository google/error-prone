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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link AssertThrowsBlockToExpression}Test */
@RunWith(JUnit4.class)
public class AssertThrowsBlockToExpressionTest {

  private final BugCheckerRefactoringTestHelper compilationHelper =
      BugCheckerRefactoringTestHelper.newInstance(AssertThrowsBlockToExpression.class, getClass());

  @Test
  public void refactoring() {
    compilationHelper
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
                assertThrows(IllegalStateException.class, () -> System.err.println());
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      int x = 1;
                      System.err.println(x);
                    });
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      if (true) {}
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
                assertThrows(IllegalStateException.class, () -> System.err.println());
                assertThrows(IllegalStateException.class, () -> System.err.println());
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      int x = 1;
                      System.err.println(x);
                    });
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      if (true) {}
                    });
              }
            }
            """)
        .allowFormattingErrors()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void comments() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      // comment1
                      System.err.println();
                      // comment2
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
                    () ->
                        // comment1
                        System.err.println()
                    // comment2
                    );
              }
            }
            """)
        .allowFormattingErrors()
        .doTest(TEXT_MATCH);
  }
}
