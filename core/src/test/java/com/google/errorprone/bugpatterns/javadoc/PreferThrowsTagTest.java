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

package com.google.errorprone.bugpatterns.javadoc;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link PreferThrowsTag} bug pattern. */
@RunWith(JUnit4.class)
public final class PreferThrowsTagTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(PreferThrowsTag.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(PreferThrowsTag.class, getClass());

  @Test
  public void replaceExceptionWithThrows() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.io.IOException;

            interface Test {
              /**
               * @exception IOException if something goes wrong
               */
              void test() throws IOException;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.io.IOException;

            interface Test {
              /**
               * @throws IOException if something goes wrong
               */
              void test() throws IOException;
            }
            """)
        .doTest();
  }

  @Test
  public void multipleExceptions() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.io.IOException;

            interface Test {
              /**
               * @exception IOException if something goes wrong
               * @exception RuntimeException if something else happens
               */
              void test() throws IOException;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.io.IOException;

            interface Test {
              /**
               * @throws IOException if something goes wrong
               * @throws RuntimeException if something else happens
               */
              void test() throws IOException;
            }
            """)
        .doTest();
  }

  @Test
  public void alreadyUsingThrows() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            import java.io.IOException;

            interface Test {
              /**
               * @throws IOException if something goes wrong
               */
              void test() throws IOException;
            }
            """)
        .doTest();
  }
}
