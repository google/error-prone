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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@test IntLongMath}Test. */
@RunWith(JUnit4.class)
public final class IntLongMathTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(IntLongMath.class, getClass());

  @Test
  public void ignoreNonWideningAssignments() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int f(int x) {",
            "     return x + 3;",
            "  }",
            "  long g(long x) {",
            "     return x + 3;",
            "  }",
            "  int h(long x) {",
            "     return (int) (x + 3);",
            "  }",
            "  long i(int x) {",
            "     // BUG: Diagnostic contains: x + 3L",
            "     return x + 3;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lambda() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  interface F {",
            "    long f(int i);",
            "  }",
            "  F f = i -> {",
            "     // BUG: Diagnostic contains: return i + 3L",
            "     return i + 3;",
            "  };",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    BugCheckerRefactoringTestHelper.newInstance(new IntLongMath(), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  static final int MILLION = 1_000_000;",
            "  long f(int x) {",
            "     long r = (x + 3) * MILLION / 3;",
            "     r = (x / 3) * MILLION / 3;",
            "     r = x / 3;",
            "     r = x + 3;",
            "     return r;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  static final int MILLION = 1_000_000;",
            "  long f(int x) {",
            "     long r = (x + 3L) * MILLION / 3;",
            "     r = (long) (x / 3) * MILLION / 3;",
            "     r = x / 3;",
            "     r = x + 3L;",
            "     return r;",
            "  }",
            "}")
        .doTest();
  }
}
