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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link MalformedInlineTag}. */
@RunWith(JUnit4.class)
public final class MalformedInlineTagTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(MalformedInlineTag.class, getClass());

  @Test
  public void positive_allInlineTags() {
    helper
        .addInputLines(
            "Test.java",
            "/** Here are a list of malformed tags: ",
            " * @{code code}",
            " * @{docRoot}",
            " * @{inheritDoc}",
            " * @{link Test}",
            " * @{linkplain Test}",
            " * @{literal literal}",
            " * @{value Test}",
            " */",
            "class Test {}")
        .addOutputLines(
            "Test.java",
            "/** Here are a list of malformed tags: ",
            " * {@code code}",
            " * {@docRoot}",
            " * {@inheritDoc}",
            " * {@link Test}",
            " * {@linkplain Test}",
            " * {@literal literal}",
            " * {@value Test}",
            " */",
            "class Test {}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void positive_withinTag() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  /** Add one to value.",
            "    * @param x an @{code int} value to increment",
            "    * @return @{code x} + 1",
            "    */",
            "  int addOne(int x) { return x+1; }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  /** Add one to value.",
            "    * @param x an {@code int} value to increment",
            "    * @return {@code x} + 1",
            "    */",
            "  int addOne(int x) { return x+1; }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void positive_atLineEnd() {
    helper
        .addInputLines(
            "Test.java",
            "/** This malformed tag spans @{code",
            " * multiple lines}.",
            " */",
            "class Test {}")
        .addOutputLines(
            "Test.java",
            "/** This malformed tag spans {@code",
            " * multiple lines}.",
            " */",
            "class Test {}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative() {
    helper
        .addInputLines(
            "Test.java", //
            "/** A correct {@link Test} tag in text. */",
            "class Test {}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative_invalidTag() {
    helper
        .addInputLines(
            "Test.java", //
            "/** This @{case} is not a known Javadoc tag. Ignore. */",
            "class Test {}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }
}
