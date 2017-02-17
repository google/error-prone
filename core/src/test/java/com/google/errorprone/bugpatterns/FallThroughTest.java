/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import com.google.errorprone.CompilationTestHelper;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link FallThrough}Test */
@RunWith(JUnit4.class)
public class FallThroughTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(FallThrough.class, getClass());

  @Test
  public void positive() throws IOException {
    testHelper.addSourceFile("FallThroughPositiveCases.java").doTest();
  }

  @Test
  public void negative() throws IOException {
    testHelper.addSourceFile("FallThroughNegativeCases.java").doTest();
  }

  @Test
  public void unnecessaryFallThrough() throws IOException {
    // make sure the "fall through" comment gets deleted
    BugCheckerRefactoringTestHelper.newInstance(new FallThrough(), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f(int x) {",
            "    switch (x) {",
            "      case 1:",
            "        break;",
            "      // fall through",
            "      case 2:",
            "        break;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f(int x) {",
            "    switch (x) {",
            "      case 1:",
            "        break;",
            "case 2:",
            "        break;",
            "    }",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
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
}
