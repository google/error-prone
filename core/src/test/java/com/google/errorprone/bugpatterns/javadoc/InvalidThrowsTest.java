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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link InvalidThrows} bug pattern. */
@RunWith(JUnit4.class)
public final class InvalidThrowsTest {
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(new InvalidThrows(), getClass());

  @Test
  public void positive() {
    refactoring
        .addInputLines(
            "Test.java",
            "import java.io.IOException;",
            "interface Test {",
            "  /**",
            "   * @throws IOException when failed",
            "   */",
            "  void foo(int a, int b);",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.io.IOException;",
            "interface Test {",
            "  /** */",
            "  void foo(int a, int b);",
            "}")
        .doTest(TestMode.TEXT_MATCH);
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
            "  void bar(int a, int b) throws IOException;",
            "  /**",
            "   * @throws IOException when failed",
            "   */",
            "  void baz(int a, int b) throws Exception;",
            "}")
        .expectUnchanged()
        .doTest(TestMode.TEXT_MATCH);
  }
}
