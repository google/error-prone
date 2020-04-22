/*
 * Copyright 2020 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link SuppressWarningsWithoutExplanation}. */
@RunWith(JUnit4.class)
public final class SuppressWarningsWithoutExplanationTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(
          new SuppressWarningsWithoutExplanation(/* emitDummyFixes= */ true), getClass());

  @Test
  public void rawTypesSuppressed() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  @SuppressWarnings(\"deprecation\")",
            "  void test() {}",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  @SuppressWarnings(\"deprecation\") // Safe because...",
            "  void test() {}",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void multipleSuppressedWarnings() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  @SuppressWarnings({\"deprecation\", \"another\"})",
            "  void test() {}",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  @SuppressWarnings({\"deprecation\", \"another\"}) // Safe because...",
            "  void test() {}",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  @SuppressWarnings(\"someotherwarning\")",
            "  void test() {}",
            "}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void hasInlineComment() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  @SuppressWarnings(\"rawtypes\") // foo",
            "  void test() {}",
            "}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suppressedOnEntiereClass() {
    helper
        .addInputLines(
            "Test.java",
            "@Deprecated",
            "@SuppressWarnings(\"deprecation\") // foo",
            "class Test {",
            "  void test() {}",
            "}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void hasCommentBefore() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  // foo",
            "  @SuppressWarnings(\"deprecation\")",
            "  void test() {}",
            "}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void hasJavadocBefore() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  /**",
            "    * Frobnicates.",
            "    * This comment might explain why it's safe.",
            "    */",
            "  @SuppressWarnings(\"deprecation\")",
            "  void test() {}",
            "}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }
}
