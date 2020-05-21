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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link AlmostJavadoc} bug pattern. */
@RunWith(JUnit4.class)
public final class AlmostJavadocTest {
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(new AlmostJavadoc(), getClass());

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(AlmostJavadoc.class, getClass());

  @Test
  public void refactoring() {
    refactoring
        .addInputLines(
            "Test.java", //
            "public class Test {",
            "  /* Foo {@link Test}. */",
            "  void foo() {}",
            "  /*",
            "   * Bar.",
            "   *",
            "   * @param bar bar",
            "   */",
            "  void bar (int bar) {}",
            "}")
        .addOutputLines(
            "Test.java", //
            "public class Test {",
            "  /** Foo {@link Test}. */",
            "  void foo() {}",
            "  /**",
            "   * Bar.",
            "   *",
            "   * @param bar bar",
            "   */",
            "  void bar (int bar) {}",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void notJavadocButNoTag() {
    helper
        .addSourceLines(
            "Test.java", //
            "public class Test {",
            "  /* Foo. */",
            "  void foo() {}",
            "}")
        .doTest();
  }

  @Test
  public void blockCommentWithMultilineClose() {
    refactoring
        .addInputLines(
            "Test.java", //
            "interface Test {",
            "  // Foo. */",
            "  void foo();",
            "  // ** Bar. */",
            "  void bar();",
            "  // /** Baz. */",
            "  void baz();",
            "}")
        .addOutputLines(
            "Test.java", //
            "interface Test {",
            "  /** Foo. */",
            "  void foo();",
            "  /** Bar. */",
            "  void bar();",
            "  /** Baz. */",
            "  void baz();",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void pathlogicalBlockCommentCases() {
    helper
        .addSourceLines(
            "Test.java", //
            "interface Test {",
            "  // */",
            "  void bar();",
            "}")
        .doTest();
  }

  @Test
  public void suppression() {
    helper
        .addSourceLines(
            "Test.java", //
            "public class Test {",
            "  /* Foo {@link Test}. */",
            "  @SuppressWarnings(\"AlmostJavadoc\")",
            "  void foo() {}",
            "}")
        .doTest();
  }

  @Test
  public void alreadyHasJavadoc_noMatch() {
    helper
        .addSourceLines(
            "Test.java", //
            "public class Test {",
            "  /** Foo. */",
            "  /* Strange extra documentation with {@link tags}. */",
            "  void foo() {}",
            "}")
        .doTest();
  }

  @Test
  public void htmlTag() {
    helper
        .addSourceLines(
            "Test.java", //
            "public class Test {",
            "  /* Foo <em>Test</em>. */",
            "  // BUG: Diagnostic contains:",
            "  void foo() {}",
            "}")
        .doTest();
  }

  @Test
  public void enumConstant() {
    helper
        .addSourceLines(
            "Test.java", //
            "public enum Test {",
            "  /* Foo <em>Test</em>. */",
            "  // BUG: Diagnostic contains:",
            "  FOO",
            "}")
        .doTest();
  }

  @Test
  public void abstractEnumConstant() {
    helper
        .addSourceLines(
            "Test.java", //
            "public enum Test {",
            "  /* Foo <em>Test</em>. */",
            "  // BUG: Diagnostic contains:",
            "  FOO {",
            "    @Override public String toString() {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void multiField() {
    helper
        .addSourceLines(
            "Test.java", //
            "public class Test {",
            "  /* Foo <em>Test</em>. */",
            "  // BUG: Diagnostic contains:",
            "  int x = 1, y = 2;",
            "}")
        .doTest();
  }
}
