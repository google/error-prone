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

/** Unit tests for {@link InvalidParam} bug pattern. */
@RunWith(JUnit4.class)
public final class InvalidParamTest {
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(InvalidParam.class, getClass());

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(InvalidParam.class, getClass());

  @Test
  public void badParameterName_positioning() {
    helper
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * @param <T> baz",
            "   // BUG: Diagnostic contains: Parameter name `c` is unknown",
            "   * @param c foo",
            "   * @param b bar",
            "   */",
            "  <T> void foo(int a, int b);",
            "}")
        .doTest();
  }

  @Test
  public void badParameterName() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * @param <T> baz",
            "   * @param c foo",
            "   * @param b bar",
            "   */",
            "  <T> void foo(int a, int b);",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * @param <T> baz",
            "   * @param a foo",
            "   * @param b bar",
            "   */",
            "  <T> void foo(int a, int b);",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void badTypeParameterName() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * @param <S> baz",
            "   * @param a bar",
            "   */",
            "  <T> void foo(int a);",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * @param <T> baz",
            "   * @param a bar",
            "   */",
            "  <T> void foo(int a);",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void verySimilarCodeParam() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * {@code foabar}, {@code barfoo}",
            "   */",
            "  void foo(int foobar);",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * {@code foobar}, {@code barfoo}",
            "   */",
            "  void foo(int foobar);",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void verySimilarCodeParam_diagnosticMessage() {
    helper
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   // BUG: Diagnostic contains: `foabar` is very close to the parameter `foobar`",
            "   * {@code foabar}, {@code barfoo}",
            "   */",
            "  void foo(int foobar);",
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
            "   * Frobnicates a {@code foobarbaz}.",
            "   * @param <T> baz",
            "   * @param a bar",
            "   * @param b quux",
            "   */",
            "  <T> void foo(int a, int b, int foobarbaz);",
            "}")
        .doTest();
  }

  @Test
  public void excludedName_noMatchDespiteSimilarParam() {
    helper
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  /** Returns {@code true}. */",
            "  boolean foo(int tree);",
            "}")
        .doTest();
  }
}
