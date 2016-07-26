/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@RunWith(JUnit4.class)
public class NarrowingCompoundAssignmentTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(NarrowingCompoundAssignment.class, getClass());
  }

  @Test
  public void testPositiveCase() throws Exception {
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
  public void testAllOps() throws Exception {
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
            "    // Right shifts are OK",
            "    s >>= 1;",
            "    s >>>= 1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase() throws Exception {
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
  public void testFloatFloat() throws Exception {
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

  @Test
  public void testPreservePrecedence() throws Exception {
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
  public void testPreservePrecedence2() throws Exception {
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
    testPrecedence("*", "*", true);
    testPrecedence("*", "+", true);
    testPrecedence("*", "<<", true);
    testPrecedence("*", "&", true);
    testPrecedence("*", "|", true);

    testPrecedence("+", "*", false);
    testPrecedence("+", "+", true);
    testPrecedence("+", "<<", true);
    testPrecedence("+", "&", true);
    testPrecedence("+", "|", true);

    testPrecedence("<<", "*", false);
    testPrecedence("<<", "+", false);
    testPrecedence("<<", "<<", true);
    testPrecedence("<<", "&", true);
    testPrecedence("<<", "|", true);

    testPrecedence("&", "*", false);
    testPrecedence("&", "+", false);
    testPrecedence("&", "<<", false);
    testPrecedence("&", "&", true);
    testPrecedence("&", "|", true);

    testPrecedence("|", "*", false);
    testPrecedence("|", "+", false);
    testPrecedence("|", "<<", false);
    testPrecedence("|", "&", false);
    testPrecedence("|", "|", true);
  }

  private void testPrecedence(String opA, String opB, boolean parens) throws Exception {
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
}
