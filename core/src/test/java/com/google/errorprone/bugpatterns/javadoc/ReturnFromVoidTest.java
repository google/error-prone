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

/** Unit tests for {@link ReturnFromVoid} bug pattern. */
@RunWith(JUnit4.class)
public final class ReturnFromVoidTest {
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(new ReturnFromVoid(), getClass());

  @Test
  public void returnsVoid() {
    refactoring
        .addInputLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * @return anything",
            "   */",
            "  void foo();",
            "}")
        .addOutputLines(
            "Test.java", //
            "interface Test {",
            "  /** */",
            "  void foo();",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void negative() {
    CompilationTestHelper.newInstance(ReturnFromVoid.class, getClass())
        .addSourceLines(
            "Test.java", //
            "interface Test {",
            "  /**",
            "   * @return anything",
            "   */",
            "  int foo();",
            "}")
        .doTest();
  }
}
