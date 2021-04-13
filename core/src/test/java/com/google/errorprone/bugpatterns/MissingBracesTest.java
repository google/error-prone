/*
 * Copyright 2021 The Error Prone Authors.
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

/** {@link MissingBraces}Test */
@RunWith(JUnit4.class)
public class MissingBracesTest {
  @Test
  public void positive() {
    BugCheckerRefactoringTestHelper.newInstance(MissingBraces.class, getClass())
        .addInputLines(
            "Test.java",
            "import java.util.List;",
            "class Test {",
            "  void f(boolean x, List<Integer> is) {",
            "    if (x) throw new AssertionError();",
            "    else x = !x;",
            "    while (x) g();",
            "    do g(); while (x);",
            "    for ( ; x; ) g();",
            "    for (int i : is) g();",
            "  }",
            "  void g() {}",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.List;",
            "class Test {",
            "  void f(boolean x, List<Integer> is) {",
            "    if (x) { throw new AssertionError(); }",
            "    else { x = !x; }",
            "    while (x) { g(); }",
            "    do { g(); } while (x);",
            "    for ( ; x; ) { g(); }",
            "    for (int i : is) { g(); }",
            "  }",
            "  void g() {}",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    CompilationTestHelper.newInstance(MissingBraces.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(boolean x) {",
            "    if (x) { g(); }",
            "    else { g(); }",
            "    while (x) { g(); }",
            "    do { g(); } while (x);",
            "    for (;;) { g(); }",
            "  }",
            "  void g() {}",
            "}")
        .doTest();
  }
}
