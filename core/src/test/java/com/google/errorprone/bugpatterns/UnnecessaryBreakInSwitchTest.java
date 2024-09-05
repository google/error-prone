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

import static com.google.common.truth.TruthJUnit.assume;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UnnecessaryBreakInSwitchTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(UnnecessaryBreakInSwitch.class, getClass());

  @Test
  public void positive() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
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
    assume().that(Runtime.version().feature()).isAtLeast(14);
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
    assume().that(Runtime.version().feature()).isAtLeast(14);
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int i) {",
            "    switch (i) {",
            "      default -> {",
            "        if (true) {",
            "          break;",
            "        }",
            "        System.err.println();",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeLabelledBreak() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
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

  @Test
  public void negativeLoop() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int i) {",
            "    while (true) {",
            "      switch (i) {",
            "        default -> {",
            "          while (true) {",
            "            break;",
            "          }",
            "        }",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveIf() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int i) {",
            "    switch (i) {",
            "      default -> {",
            "        if (true) {",
            "          // BUG: Diagnostic contains: break is unnecessary",
            "          break;",
            "        } else {",
            "          // BUG: Diagnostic contains: break is unnecessary",
            "          break;",
            "        }",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }
}
