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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link FallThrough}Test */
@RunWith(JUnit4.class)
public class FallThroughTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(FallThrough.class, getClass());

  @Test
  public void positive() {
    testHelper.addSourceFile("FallThroughPositiveCases.java").doTest();
  }

  @Test
  public void negative() {
    testHelper.addSourceFile("FallThroughNegativeCases.java").doTest();
  }

  @Test
  public void foreverLoop() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int x) {",
            "    switch (x) {",
            "      case 1:",
            "        for (;;) {}",
            "      case 2:",
            "        break;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void commentInBlock() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int x) {",
            "    switch (x) {",
            "      case 0: {",
            "        // fall through",
            "      }",
            "      case 1: {",
            "        System.err.println();",
            "        // fall through",
            "      }",
            "      case 2:",
            "        break;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void emptyBlock() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(char c, boolean b) {",
            "    switch (c) {",
            "      case 'a': {}",
            "      // fall through",
            "      default:",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
