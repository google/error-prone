/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.javadoc;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link InvalidInlineTag} bug pattern. */
@RunWith(JUnit4.class)
public final class InvalidInlineTagTest {
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(new InvalidInlineTag(), getClass());

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(InvalidInlineTag.class, getClass());

  @Test
  public void typo() {
    refactoring
        .addInputLines(
            "Test.java", //
            "interface Test {",
            "  /** @return anything {@lnk #foo} */",
            "  void foo();",
            "}")
        .addOutputLines(
            "Test.java", //
            "interface Test {",
            "  /** @return anything {@link #foo} */",
            "  void foo();",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void outsideEditDistanceLimit() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  /** {@SoFarOutsideEditLimit} */",
            "  void test() {}",
            "}")
        .doTest();
  }

  @Test
  public void isAType() {
    refactoring
        .addInputLines(
            "SomeType.java",
            "interface SomeType {",
            "  /** {@SomeType} {@com.google.common.labs.ClearlyAType} */",
            "  void foo();",
            "}")
        .addOutputLines(
            "SomeType.java",
            "interface SomeType {",
            "  /** {@link SomeType} {@link com.google.common.labs.ClearlyAType} */",
            "  void foo();",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void isAQualifiedType() {
    refactoring
        .addInputLines(
            "SomeType.java",
            "interface SomeType {",
            "  /** {@SomeType.A} */",
            "  void foo();",
            "  interface A {}",
            "}")
        .addOutputLines(
            "SomeType.java",
            "interface SomeType {",
            "  /** {@link SomeType.A} */",
            "  void foo();",
            "  interface A {}",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void parameterInlineTag() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * Provide an {@a}",
            "   */",
            "  void foo(int a);",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * Provide an {@code a}",
            "   */",
            "  void foo(int a);",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void improperParam() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /** Blah",
            "    * {@param a}, {@param b}  */",
            "  void foo(int a);",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** Blah",
            "   * {@code a}, {@param b} */",
            "  void foo(int a);",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void inlineParam() {
    helper
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  // BUG: Diagnostic contains:",
            "  /** Frobnicates a @param foo. */",
            "  void frobnicate(String foo);",
            "}")
        .doTest();
  }

  @Test
  public void inlineParamIncorrectlyWrapped() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /** Frobnicates a @code{foo}. */",
            "  void frobnicate(String foo);",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** Frobnicates a {@code foo}. */",
            "  void frobnicate(String foo);",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void parensRatherThanCurlies() {
    helper
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  // BUG: Diagnostic contains: tags: {@code",
            "  /** Frobnicates a (@code foo). */",
            "  void frobnicate(String foo);",
            "}")
        .doTest();
  }

  @Test
  public void parensRatherThanCurliesRefactoring() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /** Frobnicates a (@code foo). */",
            "  void frobnicate(String foo);",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** Frobnicates a {@code foo}. */",
            "  void frobnicate(String foo);",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void parensRatherThanCurly_matchesBraces() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /** Frobnicates a (@link #foo()). */",
            "  void frobnicate(String foo);",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** Frobnicates a {@link #foo()}. */",
            "  void frobnicate(String foo);",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }
}
