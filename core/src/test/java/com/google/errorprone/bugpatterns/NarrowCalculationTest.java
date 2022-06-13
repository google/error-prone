/*
 * Copyright 2022 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link NarrowCalculation}. */
@RunWith(JUnit4.class)
public final class NarrowCalculationTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(NarrowCalculation.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(NarrowCalculation.class, getClass());

  @Test
  public void integerDivision() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  final float a = 1 / 2;",
            "}")
        .doTest();
  }

  @Test
  public void integerDivision_actuallyInteger() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  final float a = 8 / 2;",
            "}")
        .doTest();
  }

  @Test
  public void integerDivision_fix() {
    refactoring
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  final float a = 1 / 2;",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  final float a = 1 / 2f;",
            "}")
        .doTest();
  }

  @Test
  public void longDivision_fix() {
    refactoring
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  final double a = 1 / 2L;",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  final double a = 1 / 2.0;",
            "}")
        .doTest();
  }

  @Test
  public void targetTypeInteger_noFinding() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  final int a = 1 / 2;",
            "}")
        .doTest();
  }

  @Test
  public void multiplication_doesNotOverflow() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  final long a = 2 * 100;",
            "}")
        .doTest();
  }

  @Test
  public void multiplication_wouldOverflow() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  final long a = 1_000_000_000 * 1_000_000_000;",
            "}")
        .doTest();
  }

  @Test
  public void multiplication_couldOverflow() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void t(int a) {",
            "    // BUG: Diagnostic contains: 2L * a",
            "    long b = 2 * a;",
            "  }",
            "}")
        .doTest();
  }
}
