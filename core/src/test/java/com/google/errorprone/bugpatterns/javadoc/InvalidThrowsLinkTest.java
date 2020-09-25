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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link InvalidThrows} bug pattern. */
@RunWith(JUnit4.class)
public final class InvalidThrowsLinkTest {
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(InvalidThrowsLink.class, getClass());

  @Test
  public void positive() {
    refactoring
        .addInputLines(
            "Test.java",
            "import java.io.IOException;",
            "interface Test {",
            "  /**",
            "   * @throws {@link IOException} when failed",
            "   */",
            "  void foo(int a, int b) throws IOException;",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.io.IOException;",
            "interface Test {",
            "  /**",
            "   * @throws IOException when failed",
            "   */",
            "  void foo(int a, int b) throws IOException;",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative() {
    refactoring
        .addInputLines(
            "Test.java",
            "import java.io.IOException;",
            "interface Test {",
            "  /**",
            "   * @throws IOException when failed",
            "   */",
            "  void foo(int a, int b) throws IOException;",
            "}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }
}
