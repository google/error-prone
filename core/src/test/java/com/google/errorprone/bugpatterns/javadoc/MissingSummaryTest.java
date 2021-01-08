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

/** Unit tests for {@link MissingSummary} bug pattern. */
@RunWith(JUnit4.class)
public final class MissingSummaryTest {

  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(new MissingSummary(), getClass());

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(MissingSummary.class, getClass());

  @Test
  public void replaceReturn() {
    refactoring
        .addInputLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * @param n foo",
            "   * @return n",
            "   */",
            "  int test(int n);",
            "}")
        .addOutputLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * Returns n.",
            "   *",
            "   * @param n foo",
            "   */",
            "  int test(int n);",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void replaceSee() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * @param n foo",
            "   * @see List other impl",
            "   */",
            "  void test(int n);",
            "  /**",
            "   * @param n foo",
            "   * @see List",
            "   */",
            "  void test2(int n);",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * See {@link List other impl}.",
            "   *",
            "   * @param n foo",
            "   */",
            "  void test(int n);",
            "  /**",
            "   * See {@link List}.",
            "   *",
            "   * @param n foo",
            "   */",
            "  void test2(int n);",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void publicCase() {
    helper
        .addSourceLines(
            "Test.java",
            "// BUG: Diagnostic contains: @see",
            "/** @see another thing */",
            "public interface Test {",
            "  // BUG: Diagnostic contains: @return",
            "  /** @return foo */",
            "  int test();",
            "}")
        .doTest();
  }

  @Test
  public void privateCase() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  /** @throws IllegalStateException */",
            "  private void test() {}",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * Summary line!",
            "   *",
            "   * @return n",
            "   */",
            "  int test();",
            "}")
        .doTest();
  }

  @Test
  public void negativeAnnotations() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  /** @param o thing to compare */",
            "  @Override public boolean equals(Object o) { return true; }",
            "  /** @deprecated use something else */",
            "  @Deprecated public void frobnicate() {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeConstructor() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  /** @param o thing to compare */",
            "  public Test(Object o) {}",
            "}")
        .doTest();
  }

  @Test
  public void negativePrivate() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  /** @param o thing to compare */",
            "  private Test(Object o) {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeOverride() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test implements java.util.function.Predicate<Object> {",
            "  /** @param o thing to compare */",
            "  public boolean test(Object o) { return false; }",
            "}")
        .doTest();
  }

  @Test
  public void seeWithHtmlLink() {
    helper
        .addSourceLines(
            "Test.java",
            "// BUG: Diagnostic contains:",
            "/** @see <a href=\"foo\">bar</a> */",
            "public interface Test {}")
        .doTest();
  }

  @Test
  public void emptyReturn() {
    helper
        .addSourceLines(
            "Test.java", //
            "interface Test {",
            "  // BUG: Diagnostic contains:",
            "  /** @return */",
            "  int test(int n);",
            "}")
        .doTest();
  }

  @Test
  public void emptyComment() {
    helper
        .addSourceLines(
            "Test.java", //
            "package test;",
            "/** */",
            "// BUG: Diagnostic contains: summary line is required",
            "public class Test {",
            "}")
        .doTest();
  }
}
