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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link LoopConditionChecker}Test */
@RunWith(JUnit4.class)
public class LoopConditionCheckerTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(LoopConditionChecker.class, getClass());

  @Test
  public void positive() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int h9() {",
            "    int sum = 0;",
            "    int i, j;",
            "    for (i = 0; i < 10; ++i) {",
            "      // BUG: Diagnostic contains:",
            "      for (j = 0; j < 10; ++i) {",
            "        sum += j;",
            "      }",
            "    }",
            "    return sum;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_noUpdate() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains:",
            "    for (int i = 0; i < 10; ) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int h9() {",
            "    int sum = 0;",
            "    int i, j;",
            "    for (i = 0; i < 10; ++i) {",
            "      for (j = 0; j < 10; ++i) {",
            "        sum += j;",
            "        j++;",
            "      }",
            "    }",
            "    return sum;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_forExpression() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    for (int i = 0; i < 10; i++) {",
            "      System.err.println(i);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_noVariable() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Iterator;",
            "class Test {",
            "  void f(Iterable<String> xs) {",
            "    Iterator<String> it = xs.iterator();",
            "    while (it.hasNext()) {",
            "      System.err.println(it.next());",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_noCondition() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    for (;;) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_noUpdate() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    for (int i = 0; i < 10; ) {",
            "      i++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_conditionUpdate() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    int i = 0;",
            "    while (i++ < 10) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_field() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int i = 0;",
            "  void f() {",
            "    while (i < 10) {",
            "      g();",
            "    }",
            "  }",
            "  void g() {",
            "    i++;",
            "  }",
            "}")
        .doTest();
  }
}
