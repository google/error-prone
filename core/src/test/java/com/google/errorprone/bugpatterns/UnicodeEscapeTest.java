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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link UnicodeEscape}. */
@RunWith(JUnit4.class)
public final class UnicodeEscapeTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnicodeEscape.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(UnicodeEscape.class, getClass());

  @Test
  public void printableAsciiCharacter_finding() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  private static final String FOO = \"\\u0020\";",
            "}")
        .doTest();
  }

  @Test
  public void unicodeEscapeRefactoredToLiteral() {
    refactoring
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  private static final String FOO = \"\\u0020\";",
            "  private static final String BAR = \"\\uuuuu0020\";",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  private static final String FOO = \" \";",
            "  private static final String BAR = \" \";",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void jdk8269150() {
    refactoring
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  private static final String FOO = \"\\u005c\\\\u005d\";",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  private static final String FOO = \"\\\\]\";",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void newlinesRefactored() {
    refactoring
        .addInputLines(
            "Test.java", //
            "class Test {\\u000Dprivate static final String FOO = null;",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {\rprivate static final String FOO = null;",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void nonPrintableAsciiCharacter_noFinding() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  private static final String FOO = \"\\u0312\";",
            "}")
        .doTest();
  }

  @Test
  public void extraEscapes_noFinding() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  private static final String FOO = \"\\\\u0020\";",
            "}")
        .doTest();
  }

  @Test
  public void everythingObfuscated() {
    refactoring
        .addInputLines("A.java", "\\u0063\\u006c\\u0061\\u0073\\u0073\\u0020\\u0041\\u007b\\u007d")
        .addOutputLines("A.java", "class A{}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void escapedLiteralBackslashU() {
    refactoring
        .addInputLines(
            "A.java", //
            "/** \\u005Cu */",
            "class A {}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }
}
