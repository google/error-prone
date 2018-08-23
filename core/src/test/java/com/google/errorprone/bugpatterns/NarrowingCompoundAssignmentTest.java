/*
 * Copyright 2014 The Error Prone Authors.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author cushon@google.com (Liam Miller-Cushon) */
@RunWith(JUnit4.class)
public class NarrowingCompoundAssignmentTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(NarrowingCompoundAssignment.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    short s = 0;",
            "    char t = 0;",
            "    byte u = 0;",
            "    float v = 0;",
            "    // BUG: Diagnostic contains: s = (short) (s * 1)",
            "    s *= 1;",
            "    // BUG: Diagnostic contains: t = (char) (t * 1)",
            "    t *= 1;",
            "    // BUG: Diagnostic contains: u = (byte) (u * 1)",
            "    u *= 1;",
            "    // BUG: Diagnostic contains: u = (byte) (u * 1L)",
            "    u *= 1L;",
            "    // BUG: Diagnostic contains: v = (float) (v * 1.0)",
            "    v *= 1.0;",
            "    // BUG: Diagnostic contains: v = (float) (v * 1.0d)",
            "    v *= 1.0d;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAllOps() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    short s = 0;",
            "    // BUG: Diagnostic contains: s = (short) (s * 1)",
            "    s *= 1;",
            "    // BUG: Diagnostic contains: s = (short) (s / 1)",
            "    s /= 1;",
            "    // BUG: Diagnostic contains: s = (short) (s % 1)",
            "    s %= 1;",
            "    // BUG: Diagnostic contains: s = (short) (s + 1)",
            "    s += 1;",
            "    // BUG: Diagnostic contains: s = (short) (s - 1)",
            "    s -= 1;",
            "    // BUG: Diagnostic contains: s = (short) (s << 1)",
            "    s <<= 1;",
            "    // Signed right shifts are OK",
            "    s >>= 1;",
            "    // BUG: Diagnostic contains: s = (short) (s >>> 1)",
            "    s >>>= 1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDeficientRightShift() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    // BUG: Diagnostic contains: i = (short) (i >>> 1)",
            "    for (short i = -1; i != 0; i >>>= 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    int s = 0;",
            "    long t = 0;",
            "    double u = 0;",
            "    s *= 1;",
            "    t *= 1;",
            "    u *= 1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFloatFloat() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    float a = 0;",
            "    float b = 0;",
            "    Float c = Float.valueOf(0);",
            "    a += b;",
            "    a += c;",
            "  }",
            "}")
        .doTest();
  }

  // bit twiddling deficient types with masks of the same width is fine
  @Test
  public void testBitTwiddle() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    short smask = 0b1;",
            "    byte bmask = 0b1;",
            "",
            "    short s = 0;",
            "    byte b = 0;",
            "",
            "    s &= smask;",
            "    s |= smask;",
            "    s ^= smask;",
            "",
            "    s &= bmask;",
            "    s |= bmask;",
            "    s ^= bmask;",
            "",
            "    b &= bmask;",
            "    b |= bmask;",
            "    b ^= bmask;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void allowsBinopsOfDeficientTypes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    short smask = 0b1;",
            "    byte bmask = 0b1;",
            "",
            "    short s = 0;",
            "    byte b = 0;",
            "",
            "    s += smask;",
            "    s -= smask;",
            "    s *= smask;",
            "",
            "    s += bmask;",
            "    s -= bmask;",
            "    s *= bmask;",
            "",
            "    b -= bmask;",
            "    b += bmask;",
            "    b /= bmask;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPreservePrecedence() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    float f = 0;",
            "    // BUG: Diagnostic contains: f = (float) (f - (3.0 - 2.0))",
            "    f -= 3.0 - 2.0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPreservePrecedence2() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    float f = 0;",
            "    // BUG: Diagnostic contains: f = (float) (f - 3.0 * 2.0)",
            "    f -= 3.0 * 2.0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPreservePrecedenceExhaustive() throws Exception {
    testPrecedence("*", "*", /* parens= */ true);
    testPrecedence("*", "+", /* parens= */ true);
    testPrecedence("*", "<<", /* parens= */ true);
    testPrecedence("*", "&", /* parens= */ true);
    testPrecedence("*", "|", /* parens= */ true);

    testPrecedence("+", "*", /* parens= */ false);
    testPrecedence("+", "+", /* parens= */ true);
    testPrecedence("+", "<<", /* parens= */ true);
    testPrecedence("+", "&", /* parens= */ true);
    testPrecedence("+", "|", /* parens= */ true);

    testPrecedence("<<", "*", /* parens= */ false);
    testPrecedence("<<", "+", /* parens= */ false);
    testPrecedence("<<", "<<", /* parens= */ true);
    testPrecedence("<<", "&", /* parens= */ true);
    testPrecedence("<<", "|", /* parens= */ true);

    testPrecedence("&", "*", /* parens= */ false);
    testPrecedence("&", "+", /* parens= */ false);
    testPrecedence("&", "<<", /* parens= */ false);
    testPrecedence("&", "&", /* parens= */ true);
    testPrecedence("&", "|", /* parens= */ true);

    testPrecedence("|", "*", /* parens= */ false);
    testPrecedence("|", "+", /* parens= */ false);
    testPrecedence("|", "<<", /* parens= */ false);
    testPrecedence("|", "&", /* parens= */ false);
    testPrecedence("|", "|", /* parens= */ true);
  }

  private void testPrecedence(String opA, String opB, boolean parens) {
    String rhs = String.format("1 %s 2", opB);
    if (parens) {
      rhs = "(" + rhs + ")";
    }
    String expect = String.format("s = (short) (s %s %s", opA, rhs);

    String compoundAssignment = String.format("    s %s= 1 %s 2;", opA, opB);

    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    short s = 0;",
            "    // BUG: Diagnostic contains: " + expect,
            compoundAssignment,
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDoubleLong() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    long a = 1;",
            "    double b = 2;",
            "    // BUG: Diagnostic contains: Compound assignments from double to long",
            "    a *= b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDoubleInt() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    int a = 1;",
            "    double b = 2;",
            "    // BUG: Diagnostic contains: Compound assignments from double to int",
            "    a *= b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFloatLong() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    long a = 1;",
            "    float b = 2;",
            "    // BUG: Diagnostic contains: Compound assignments from float to long",
            "    a *= b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFloatInt() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    int a = 1;",
            "    float b = 2;",
            "    // BUG: Diagnostic contains:" + " Compound assignments from float to int",
            "    a *= b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void exhaustiveTypes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(short s, byte b, char c, int i, long l, float f, double d) {",
            "    s += s;",
            "    s += b;",
            "    // BUG: Diagnostic contains:",
            "    s += c;",
            "    // BUG: Diagnostic contains:",
            "    s += i;",
            "    // BUG: Diagnostic contains:",
            "    s += l;",
            "    // BUG: Diagnostic contains:",
            "    s += f;",
            "    // BUG: Diagnostic contains:",
            "    s += d;",
            "    // BUG: Diagnostic contains:",
            "    b += s;",
            "    b += b;",
            "    // BUG: Diagnostic contains:",
            "    b += c;",
            "    // BUG: Diagnostic contains:",
            "    b += i;",
            "    // BUG: Diagnostic contains:",
            "    b += l;",
            "    // BUG: Diagnostic contains:",
            "    b += f;",
            "    // BUG: Diagnostic contains:",
            "    b += d;",
            "    // BUG: Diagnostic contains:",
            "    c += s;",
            "    // BUG: Diagnostic contains:",
            "    c += b;",
            "    c += c;",
            "    // BUG: Diagnostic contains:",
            "    c += i;",
            "    // BUG: Diagnostic contains:",
            "    c += l;",
            "    // BUG: Diagnostic contains:",
            "    c += f;",
            "    // BUG: Diagnostic contains:",
            "    c += d;",
            "    i += s;",
            "    i += b;",
            "    i += c;",
            "    i += i;",
            "    // BUG: Diagnostic contains:",
            "    i += l;",
            "    // BUG: Diagnostic contains:",
            "    i += f;",
            "    // BUG: Diagnostic contains:",
            "    i += d;",
            "    l += s;",
            "    l += b;",
            "    l += c;",
            "    l += i;",
            "    l += l;",
            "    // BUG: Diagnostic contains:",
            "    l += f;",
            "    // BUG: Diagnostic contains:",
            "    l += d;",
            "    f += s;",
            "    f += b;",
            "    f += c;",
            "    f += i;",
            "    f += l;",
            "    f += f;",
            "    // BUG: Diagnostic contains:",
            "    f += d;",
            "    d += s;",
            "    d += b;",
            "    d += c;",
            "    d += i;",
            "    d += l;",
            "    d += f;",
            "    d += d;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testBoxing() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    int a = 1;",
            "    // BUG: Diagnostic contains: from Long to int",
            "    a += (Long) 0L;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testStringConcat() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    String a = \"\";",
            "    a += (char) 0;",
            "  }",
            "}")
        .doTest();
  }
}
