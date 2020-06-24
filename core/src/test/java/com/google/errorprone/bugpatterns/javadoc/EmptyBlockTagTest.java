/*
 * Copyright 2019 The Error Prone Authors.
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

/** Unit tests for {@link EmptyBlockTag} bug pattern. */
@RunWith(JUnit4.class)
public final class EmptyBlockTagTest {
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(new EmptyBlockTag(), getClass());
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(EmptyBlockTag.class, getClass());

  @Test
  public void removes_emptyParam() {
    refactoring
        .addInputLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * @param p",
            "   */",
            "  void foo(int p);",
            "}")
        .addOutputLines(
            "Test.java", //
            "interface Test {",
            "  /** */",
            "  void foo(int p);",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void removes_emptyThrows() {
    refactoring
        .addInputLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * @throws Exception",
            "   */",
            "  void foo() throws Exception;",
            "}")
        .addOutputLines(
            "Test.java", //
            "interface Test {",
            "  /** */",
            "  void foo() throws Exception;",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void removes_emptyReturn() {
    refactoring
        .addInputLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * @return",
            "   */",
            "  int foo();",
            "}")
        .addOutputLines(
            "Test.java", //
            "interface Test {",
            "  /** */",
            "  int foo();",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void removes_emptyDeprecated() {
    refactoring
        .addInputLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * @deprecated",
            "   */",
            "  int foo();",
            "}")
        .expectUnchanged()
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void removes_emptyDeprecatedOnClass() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "/**",
            "  // BUG: Diagnostic contains:",
            " * @deprecated",
            " */",
            "@Deprecated",
            "interface Test {",
            "  void foo();",
            "}")
        .doTest();
  }

  @Test
  public void removes_emptyParamOnClass() {
    refactoring
        .addInputLines(
            "Test.java", //
            "/**",
            " * @param <T>",
            " */",
            "interface Test<T> {",
            "  T foo();",
            "}")
        .addOutputLines(
            "Test.java", //
            "/** */",
            "interface Test<T> {",
            "  T foo();",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void removes_emptyAllTheThings() {
    refactoring
        .addInputLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * @param p",
            "   * @return",
            "   * @throws Exception",
            "   */",
            "  @Deprecated",
            "  int foo(int p) throws Exception;",
            "}")
        .addOutputLines(
            "Test.java", //
            "interface Test {",
            "  /** */",
            "  @Deprecated",
            "  int foo(int p) throws Exception;",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void keeps_paramWithDescription() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * @param p is important",
            "   */",
            "  void foo(int p);",
            "}")
        .doTest();
  }

  @Test
  public void keeps_throwsWithDescription() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * @throws Exception because blah",
            "   */",
            "  void foo() throws Exception;",
            "}")
        .doTest();
  }

  @Test
  public void keeps_returnWithDescription() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * @return A value",
            "   */",
            "  int foo();",
            "}")
        .doTest();
  }

  @Test
  public void keeps_deprecatedWithDescription() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * @deprecated Very old",
            "   */",
            "  @Deprecated",
            "  void foo();",
            "}")
        .doTest();
  }

  @Test
  public void keeps_allTheThingsWithDescriptions() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * @param p is important",
            "   * @return a value",
            "   * @throws Exception because",
            "   * @deprecated Very old",
            "   */",
            "  @Deprecated",
            "  int foo(int p) throws Exception;",
            "}")
        .doTest();
  }

  @Test
  public void keeps_deprecatedOnClass() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "/**",
            " * @deprecated Use other Test2 instead",
            " */",
            "@Deprecated",
            "interface Test {",
            "  void foo();",
            "}")
        .doTest();
  }

  @Test
  public void keeps_paramOnClass() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "/**",
            " * @param <T> the input type param",
            " */",
            "interface Test<T> {",
            "  T foo();",
            "}")
        .doTest();
  }

  @Test
  public void keeps_whenSuppressed() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * @param p",
            "   */",
            "  @SuppressWarnings(\"EmptyBlockTag\")",
            "  void foo(int p);",
            "}")
        .doTest();
  }
}
