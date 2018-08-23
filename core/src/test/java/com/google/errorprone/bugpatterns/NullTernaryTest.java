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

/** {@link NullTernary}Test */
@RunWith(JUnit4.class)
public class NullTernaryTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(NullTernary.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(boolean b) {",
            "    // BUG: Diagnostic contains:",
            "    int x = b ? 0 : null;",
            "    // BUG: Diagnostic contains:",
            "    long l = b ? null : 0;",
            "    // BUG: Diagnostic contains:",
            "    g(\"\", b ? null : 0);",
            "    // BUG: Diagnostic contains:",
            "    h(\"\", 1, b ? null : 0);",
            "    // BUG: Diagnostic contains:",
            "    h(\"\", 1, b ? null : 0, 3);",
            "    // BUG: Diagnostic contains:",
            "    int z = 0 + (b ? null : 1);",
            "    // BUG: Diagnostic contains:",
            "    z = (b ? null : 1) + 0;",
            "  }",
            "  void g(String s, int y) {}",
            "  void h(String s, int... y) {}",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(boolean b) {",
            "    int x = b ? 0 : 1;",
            "    Integer y = b ? 0 : null;",
            "    g(\"\", b ? 1 : 0);",
            "    h(\"\", 1, b ? 1 : 0);",
            "    h(\"\", 1, b ? 1 : 0, 3);",
            "    int z = 0 + (b ? 0 : 1);",
            "    boolean t = (b ? 0 : null) == Integer.valueOf(0);",
            "  }",
            "  void g(String s, int y) {}",
            "  void h(String s, int... y) {}",
            "}")
        .doTest();
  }

  @Test
  public void lambdas() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  interface I {",
            "    int f();",
            "  }",
            "  interface J {",
            "    Integer f();",
            "  }",
            "  interface K<X> {",
            "    X f();",
            "  }",
            "  void f(boolean b) {",
            "    // BUG: Diagnostic contains:",
            "    I i = () -> { return b ? null : 1; };",
            "    J j = () -> { return b ? null : 1; };",
            "    K<Integer> k = () -> { return b ? null : 1; };",
            "    // BUG: Diagnostic contains:",
            "    i = () -> b ? null : 1;",
            "    j = () -> b ? null : 1;",
            "    k = () -> b ? null : 1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void conditionalInCondition() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void conditionalInCondition(Object array, String input) {",
            "    int arrayDimensions = ",
            "        ((array!=null?input:null) == null)",
            "            ? 0",
            "            : (array!=null?input:null).length();",
            "  }",
            "}")
        .doTest();
  }
}
