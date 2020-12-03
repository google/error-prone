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

/** Unit tests for {@link InvalidBlockTag} bug pattern. */
@RunWith(JUnit4.class)
public final class InvalidBlockTagTest {
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(new InvalidBlockTag(), getClass());

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(InvalidBlockTag.class, getClass());

  @Test
  public void typo() {
    refactoring
        .addInputLines(
            "Test.java", //
            "interface Test {",
            "  /** @returns anything */",
            "  void foo();",
            "}")
        .addOutputLines(
            "Test.java", //
            "interface Test {",
            "  /** @return anything */",
            "  void foo();",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void veryBadTypo_noSuggestion() {
    refactoring
        .addInputLines(
            "Test.java", //
            "interface Test {",
            "  /** @returnFnargleBlargle anything */",
            "  void foo();",
            "}")
        .expectUnchanged()
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void otherAcceptedTags() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  /** @hide */",
            "  void test() {}",
            "}")
        .doTest();
  }

  @Test
  public void inHtml() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * <code>",
            "   *    @Override",
            "   *    boolean equals(Object o);",
            "   *  </code>",
            "   * @See Test",
            "   * <pre>",
            "   *    @Override",
            "   *    boolean equals(Object o);",
            "   *  </pre>",
            "   */",
            "  void bar();",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * <code>",
            "   *    {@literal @}Override",
            "   *    boolean equals(Object o);",
            "   *  </code>",
            "   * @see Test",
            "   * <pre>",
            "   *    {@literal @}Override",
            "   *    boolean equals(Object o);",
            "   *  </pre>",
            "   */",
            "  void bar();",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void parameterBlockTag() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * @a blah",
            "   */",
            "  void foo(int a);",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /**",
            "   * @param a blah",
            "   */",
            "  void foo(int a);",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void inheritDoc() {
    refactoring
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /** @inheritDoc */",
            "  void frobnicate(String foo);",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** {@inheritDoc} */",
            "  void frobnicate(String foo);",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void java8Tags() {
    helper
        .addSourceLines(
            "Test.java",
            "/**",
            "  * @apiNote does nothing",
            "  * @implNote not implemented",
            "  */",
            "class Test {}")
        .doTest();
  }
}
