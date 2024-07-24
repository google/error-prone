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
public class UnnecessaryBreakInSwitchTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(UnnecessaryBreakInSwitch.class, getClass());

  @Test
  public void positive() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int i) {",
            "    switch (i) {",
            "      default -> {",
            "        // BUG: Diagnostic contains: break is unnecessary",
            "        break;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeTraditional() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int i) {",
            "    switch (i) {",
            "      default:",
            "        break;",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeEmpty() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int i) {",
            "    switch (i) {",
            "      default -> {",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeNotLast() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int i) {",
            "    switch (i) {",
            "      default -> {",
            "        if (true) break;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeLabelledBreak() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int i) {",
            "    outer:",
            "    while (true) {",
            "      switch (i) {",
            "        default -> {",
            "          break outer;",
            "        }",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
