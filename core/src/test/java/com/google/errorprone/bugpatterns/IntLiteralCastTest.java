/*
 * Copyright 2025 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public class IntLiteralCastTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(IntLiteralCast.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(IntLiteralCast.class, getClass());

  @Test
  public void positive() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.Arrays;
            import java.util.List;

            class Test {
              void f() {
                long l = (long) 1 << 32;
                l = (long) 0;
                float f = 1.0f;
                double d = 1.0;
                List<Float> floats = Arrays.asList((float) 0, (float) 1, (float) 2);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Arrays;
            import java.util.List;

            class Test {
              void f() {
                long l = 1L << 32;
                l = 0L;
                float f = 1f;
                double d = 1d;
                List<Float> floats = Arrays.asList(0.0f, 1.0f, 2.0f);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f() {
                long l = (int) 1 << 32;
                long m = (long) 1L << 32;
                int i = 42;
                long n = (long) i << 32;
                double d = (double) 042;
                d = (double) 0x42;
                d = (double) 0b10;
              }
            }
            """)
        .doTest();
  }
}
