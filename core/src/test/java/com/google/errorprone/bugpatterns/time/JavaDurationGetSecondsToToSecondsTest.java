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
package com.google.errorprone.bugpatterns.time;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link JavaDurationGetSecondsToToSeconds}. */
@RunWith(JUnit4.class)
public class JavaDurationGetSecondsToToSecondsTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JavaDurationGetSecondsToToSeconds.class, getClass());

  private final BugCheckerRefactoringTestHelper refactorHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          JavaDurationGetSecondsToToSeconds.class, getClass());

  @Test
  public void testNegative() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import java.time.Duration;

            public class TestCase {
              public static void foo(Duration duration) {
                long seconds = duration.getSeconds();
                int nanos = duration.getNano();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void testPositive() {
    refactorHelper
        .addInputLines(
            "test/TestCase.java",
            """
            package test;

            import java.time.Duration;

            public class TestCase {
              public static void foo(Duration duration) {
                long seconds = duration.getSeconds();
              }
            }
            """)
        .addOutputLines(
            "out/TestCase.java",
            """
            package test;

            import java.time.Duration;

            public class TestCase {
              public static void foo(Duration duration) {
                long seconds = duration.toSeconds();
              }
            }
            """)
        .doTest();
  }
}
