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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link UnicodeInCode}. */
@RunWith(JUnit4.class)
public final class UnicodeInCodeTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnicodeInCode.class, getClass());

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  final int noUnicodeHereBoss = 1;",
            "}")
        .doTest();
  }

  @Test
  public void negativeInComment() {
    helper
        .addSourceLines(
            "Test.java", //
            "/** \u03C0 */",
            "class Test {",
            "  final int noUnicodeHereBoss = 1; // roughly \u03C0",
            "}")
        .doTest();
  }

  @Test
  public void negativeInStringLiteral() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  static final String pi = \"\u03C0\";",
            "}")
        .doTest();
  }

  @Test
  public void negativeInCharLiteral() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  static final char pi = '\u03C0';",
            "}")
        .doTest();
  }

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  static final double \u03C0 = 3;",
            "}")
        .doTest();
  }
}
