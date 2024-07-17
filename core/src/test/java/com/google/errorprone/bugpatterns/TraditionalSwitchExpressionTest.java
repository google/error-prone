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

import static org.junit.Assume.assumeTrue;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TraditionalSwitchExpressionTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(TraditionalSwitchExpression.class, getClass());

  @Test
  public void positive() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int f(int i) {",
            "    // BUG: Diagnostic contains: Prefer -> switches for switch expressions",
            "    return switch (i) {",
            "      default:",
            "        yield -1;",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeStatement() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int i) {",
            "    switch (i) {",
            "      default:",
            "        return;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeArrowStatement() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int i) {",
            "    switch (i) {",
            "      default -> System.err.println();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeArrow() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int f(int i) {",
            "    return switch (i) {",
            "      default -> -1;",
            "    };",
            "  }",
            "}")
        .doTest();
  }
}
