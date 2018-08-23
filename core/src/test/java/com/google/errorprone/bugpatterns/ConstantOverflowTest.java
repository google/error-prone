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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ConstantOverflow}Test */
@RunWith(JUnit4.class)
public class ConstantOverflowTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(ConstantOverflow.class, getClass());

  @Test
  public void positiveExpressions() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static final int C = 1;",
            "  void g(int x) {}",
            "  void f() {",
            "    // BUG: Diagnostic contains: if (1000L * 1000 * 1000 * 10 * 1L == 0);",
            "    if (1000 * 1000 * 1000 * 10 * 1L == 0);",
            "    // BUG: Diagnostic contains: int x = (int) (1000L * 1000 * 1000 * 10 * 1L);",
            "    int x = (int) (1000 * 1000 * 1000 * 10 * 1L);",
            "    // BUG: Diagnostic contains: long y = 1000L * 1000 * 1000 * 10;",
            "    int y = 1000 * 1000 * 1000 * 10;",
            "    // BUG: Diagnostic contains:",
            "    g(C * 1000 * 1000 * 1000 * 10);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveFields() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: Long a = 1000L * 1000 * 1000 * 10 * 1L;",
            "  Long a = 1000 * 1000 * 1000 * 10 * 1L;",
            "  // BUG: Diagnostic contains:",
            "  int b = (int) 24L * 60 * 60 * 1000 * 1000;",
            "  long c = (long) 24L * 60 * 60 * 1000 * 1000;",
            "  // BUG: Diagnostic contains: long d = 24L * 60 * 60 * 1000 * 1000;",
            "  long d = 24 * 60 * 60 * 1000 * 1000;",
            "}")
        .doTest();
  }

  @Test
  public void negativeFloat() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  public static final int a = (int) (10 / 0.5);",
            "}")
        .doTest();
  }

  @Test
  public void negativeCharCast() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public static final char a = (char) Integer.MAX_VALUE;",
            "  public static final char b = (char) -1;",
            "  public static final char c = (char) 1;",
            "}")
        .doTest();
  }

  @Test
  public void negativeCast() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public static final byte a = (byte) 1;",
            "  private static final byte b = (byte) 0b1000_0000;",
            "  private static final byte c = (byte) 0x80;",
            "  private static final byte d = (byte) 0xfff;",
            "  private static final byte e = (byte) -1;",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  long microDay = 24L * 60 * 60 * 1000 * 1000;",
            "}")
        .doTest();
  }

  @Test
  public void bitAnd() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private static final int MASK = (1 << 31);",
            "  void f(int[] xs) {",
            "    for (final int x : xs) {",
            "      final int y = (x & (MASK - 1));",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void longOverflow() {
    BugCheckerRefactoringTestHelper.newInstance(new ConstantOverflow(), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  private static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;",
            "  void f() {",
            "    System.err.println(2 * GOLDEN_GAMMA);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  private static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;",
            "  void f() {",
            "    System.err.println(2 * GOLDEN_GAMMA);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void onlyFixIntegers() {
    BugCheckerRefactoringTestHelper.newInstance(new ConstantOverflow(), getClass())
        .addInputLines("in/Test.java", "class Test {", "  int a = 'a' + Integer.MAX_VALUE;", "}")
        .addOutputLines("out/Test.java", "class Test {", "  int a = 'a' + Integer.MAX_VALUE;", "}")
        .doTest();
  }
}
