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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;
import static org.junit.Assume.assumeTrue;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
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
  public void badColon() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * @param c: foo",
            "   */",
            "  <T> void foo(int c);",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * @param c foo",
            "   */",
            "  <T> void foo(int c);",
            "}")
        .doTest(TEXT_MATCH);
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
        .doTest(TEXT_MATCH);
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
        .doTest(TEXT_MATCH);
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
        .doTest(TEXT_MATCH);
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

  @Test
  public void negative_record() {
    assumeTrue(RuntimeVersion.isAtLeast16());
    helper
        .addSourceLines(
            "Test.java", //
            "/**",
            " * @param name Name.",
            " */",
            "public record Test(String name) {}")
        .doTest();
  }

  @Test
  public void badParameterName_record() {
    assumeTrue(RuntimeVersion.isAtLeast16());
    helper
        .addSourceLines(
            "Test.java",
            "/**",
            " // BUG: Diagnostic contains: Parameter name `bar` is unknown",
            " * @param bar Foo.",
            " */",
            "public record Test(String foo) {}")
        .doTest();
  }

  @Test
  public void multipleConstructors_record() {
    assumeTrue(RuntimeVersion.isAtLeast16());
    helper
        .addSourceLines(
            "Test.java",
            "/**",
            " * @param foo Foo.",
            " * @param bar Bar.",
            " */",
            "public record Test(String foo, Integer bar) {",
            "  public Test(Integer bar) {",
            "    this(null, bar);",
            "  }",
            "",
            "  /**",
            "   // BUG: Diagnostic contains: Parameter name `bar` is unknown",
            "   * @param bar Foo.",
            "   */",
            "  public Test(String foo) {",
            "    this(foo, null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void typeParameter_record() {
    assumeTrue(RuntimeVersion.isAtLeast16());
    helper
        .addSourceLines(
            "Negative.java",
            "/**",
            " * @param <T> The type parameter.",
            " * @param contents Contents.",
            " * @param bar Bar.",
            " */",
            "public record Negative<T>(T contents, String bar) {}")
        .addSourceLines(
            "Positive.java",
            "/**",
            " // BUG: Diagnostic contains: Parameter name `E` is unknown",
            " * @param <E> The type parameter.",
            " * @param contents Contents.",
            " * @param bar Bar.",
            " */",
            "public record Positive<T>(T contents, String bar) {}")
        .doTest();
  }

  @Test
  public void compactConstructor_record() {
    assumeTrue(RuntimeVersion.isAtLeast16());
    helper
        .addSourceLines(
            "Test.java",
            "/**",
            " * @param name Name.",
            " */",
            "public record Test(String name) {",
            "  public Test {}",
            "}")
        .doTest();
  }

  @Test
  public void normalConstructor_record() {
    assumeTrue(RuntimeVersion.isAtLeast16());
    helper
        .addSourceLines(
            "Test.java",
            "/**",
            " * @param name Name.",
            " */",
            "public record Test(String name) {",
            "  /**",
            "   // BUG: Diagnostic contains: Parameter name `foo` is unknown",
            "   * @param foo Name.",
            "   */",
            "  public Test(String name) {",
            "    this.name = name;",
            "  }",
            " }")
        .doTest();
  }
}
