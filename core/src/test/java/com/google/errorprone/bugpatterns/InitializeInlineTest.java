/*
 * Copyright 2020 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link InitializeInline}. */
@RunWith(JUnit4.class)
public final class InitializeInlineTest {
  private final BugCheckerRefactoringTestHelper compilationHelper =
      BugCheckerRefactoringTestHelper.newInstance(InitializeInline.class, getClass());

  @Test
  public void simple() {
    compilationHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    int a;",
            "    a = 1;",
            "    final int b;",
            "    b = 1;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    int a = 1;",
            "    final int b = 1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void multipleAssignment_noMatch() {
    compilationHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    int a;",
            "    a = 1;",
            "    a = 2;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void multipleAssignment_withinBlock() {
    compilationHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  int test() {",
            "    int a;",
            "    if (true) {",
            "      a = 1;",
            "      return a;",
            "    }",
            "    a = 2;",
            "    return a;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void assignedWithinTry_noMatch() {
    compilationHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  int test() {",
            "    int c;",
            "    try {",
            "      c = 1;",
            "    } catch (Exception e) {",
            "      throw e;",
            "    }",
            "    return c;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void unstylishBlocks() {
    compilationHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  int test() {",
            "    int c;",
            "    if (hashCode() == 0) c = 1;",
            "    else c = 2;",
            "    return c;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
