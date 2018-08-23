/*
 * Copyright 2016 The Error Prone Authors.
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

/** {@link IdentityBinaryExpression}Test */
@RunWith(JUnit4.class)
public class IdentityBinaryExpressionTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(IdentityBinaryExpression.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  boolean f(boolean a) {",
            "    // BUG: Diagnostic contains:",
            "    return a || a;",
            "  }",
            "  boolean g(boolean a) {",
            "    // BUG: Diagnostic contains:",
            "    return a && a;",
            "  }",
            "  boolean h(boolean a) {",
            "    // BUG: Diagnostic contains:",
            "    return f(a) && f(a);",
            "  }",
            "  boolean i(boolean a) {",
            "    boolean r;",
            "    // BUG: Diagnostic contains:",
            "    r = a & a;",
            "    // BUG: Diagnostic contains:",
            "    r = a | a;",
            "    return r;",
            "  }",
            "  int j(int x) {",
            "    int r;",
            "    // BUG: Diagnostic contains:",
            "    r = x & x;",
            "    // BUG: Diagnostic contains:",
            "    r = x | x;",
            "    return x;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  boolean f(boolean a, boolean b) {",
            "    return a || b;",
            "  }",
            "  boolean g(boolean a, boolean b) {",
            "    return a && b;",
            "  }",
            "  boolean h(boolean a, boolean b) {",
            "    return a & b;",
            "  }",
            "  boolean i(boolean a, boolean b) {",
            "    return a | b;",
            "  }",
            "  int j(int a, int b) {",
            "    return a & b;",
            "  }",
            "  int k(int a, int b) {",
            "    return a | b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeLiteral() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  double f() {",
            "    return 1.0 / 1.0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fixes() {
    compilationHelper
        .addSourceLines(
            "in/Test.java", //
            "class Test {",
            "  void f(int a) {",
            "    // BUG: Diagnostic contains: equivalent to `1`",
            "    int r = a / a;",
            "    // BUG: Diagnostic contains: equivalent to `0`",
            "    r = a - a;",
            "    // BUG: Diagnostic contains: equivalent to `0`",
            "    r = a % a;",
            "    // BUG: Diagnostic contains: equivalent to `true`",
            "    boolean b = a >= a;",
            "    // BUG: Diagnostic contains: equivalent to `true`",
            "    b = a == a;",
            "    // BUG: Diagnostic contains: equivalent to `true`",
            "    b = a <= a;",
            "    // BUG: Diagnostic contains: equivalent to `false`",
            "    b = a > a;",
            "    // BUG: Diagnostic contains: equivalent to `false`",
            "    b = a < a;",
            "    // BUG: Diagnostic contains: equivalent to `false`",
            "    b = a != a;",
            "    // BUG: Diagnostic contains: equivalent to `false`",
            "    b = b ^ b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeAssert() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static org.junit.Assert.assertTrue;",
            "import static org.junit.Assert.assertFalse;",
            "class Test {",
            "  void f(int x) {",
            "    assertTrue(x == x);",
            "    assertFalse(x != x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isNaN() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  boolean f(float a, Float b, double c, Double d) {",
            "    boolean r = false;",
            "    // BUG: Diagnostic contains: equivalent to `Float.isNaN(a)`",
            "    r |= a == a;",
            "    // BUG: Diagnostic contains: equivalent to `!Float.isNaN(a)`",
            "    r |= a != a;",
            "    // BUG: Diagnostic contains: equivalent to `Float.isNaN(b)`",
            "    r |= b == b;",
            "    // BUG: Diagnostic contains: equivalent to `!Float.isNaN(b)`",
            "    r |= b != b;",
            "    // BUG: Diagnostic contains: equivalent to `Double.isNaN(c)`",
            "    r |= c == c;",
            "    // BUG: Diagnostic contains: equivalent to `!Double.isNaN(c)`",
            "    r |= c != c;",
            "    // BUG: Diagnostic contains: equivalent to `Double.isNaN(d)`",
            "    r |= d == d;",
            "    // BUG: Diagnostic contains: equivalent to `!Double.isNaN(d)`",
            "    r |= d != d;",
            "    return r;",
            "  }",
            "}")
        .doTest();
  }
}
