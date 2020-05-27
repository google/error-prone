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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link InvalidLink} bug pattern. */
@RunWith(JUnit4.class)
public final class InvalidLinkTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(InvalidLink.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(new InvalidLink(), getClass());

  @Test
  public void httpLink() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /** {@link http://foo/bar/baz} */",
            "  void foo();",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** <a href=\"http://foo/bar/baz\">link</a> */",
            "  void foo();",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void httpLink_lineBreak() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /** {@link http://foo/bar/baz",
            "   * foo}",
            "   */",
            "  void foo();",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** <a href=\"http://foo/bar/baz\">foo</a> */",
            "  void foo();",
            "}")
        .doTest(TEXT_MATCH);
  }


  @Test
  public void badMethodLink() {
    helper
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  // BUG: Diagnostic contains: The reference `#bar()` to a method doesn't resolve",
            "  /** {@link #bar()} */",
            "  void foo();",
            "}")
        .doTest();
  }

  @Test
  public void validLinks() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "interface Test {",
            "  /** {@link Test} {@link List} {@link IllegalArgumentException}",
            "    * {@link #foo} {@link #foo()} */",
            "  void foo();",
            "}")
        .doTest();
  }

  @Test
  public void dontComplainAboutFullyQualified() {
    helper
        .addSourceLines(
            "Test.java", //
            "/** {@link i.dont.exist.B#foo} */",
            "interface A {}")
        .doTest();
  }

  @Test
  public void shouldBeMethodLink() {
    refactoring
        .addInputLines(
            "Test.java", //
            "/** {@link frobnicate} */",
            "interface A {}")
        .addOutputLines(
            "Test.java", //
            "/** {@link #frobnicate} */",
            "interface A {}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void shouldBeParameterReference() {
    refactoring
        .addInputLines(
            "Test.java",
            "class Test {",
            "  /** Pass in a {@link bar} */",
            "  void foo(String bar) {}",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  /** Pass in a {@code bar} */",
            "  void foo(String bar) {}",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void multiField() {
    helper
        .addSourceLines(
            "Param.java", //
            "@interface Param {",
            "  String name() default \"\";",
            "}")
        .addSourceLines(
            "Test.java", //
            "@interface Test {",
            "  /** Pass in a {@link Tuple<Object>} */",
            "  Param extraPositionals() default @Param(name = \"\");",
            "}")
        .doTest();
  }

  @Test
  public void emptyLinkTest() {
    helper
        .addSourceLines(
            "Test.java", //
            "interface Test {",
            "  /** {@link} */",
            "  void foo();",
            "}")
        .doTest();
  }
}
