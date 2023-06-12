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

/** {@link LongDoubleConversion}Test */
@RunWith(JUnit4.class)
public class LongDoubleConversionTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(LongDoubleConversion.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(LongDoubleConversion.class, getClass());

  @Test
  public void losesPrecision() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void method(Long l) {}",
            "  void method(double l) {}",
            "  {",
            "    // BUG: Diagnostic contains:",
            "    method(9223372036854775806L);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void doesNotActuallyLosePrecision_noFinding() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void method(Long l) {}",
            "  void method(double l) {}",
            "  {",
            "    method(0L);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void explicitlyCastToDouble_noFinding() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void method(double l) {}",
            "  {",
            "    method((double) 0L);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactors_simpleArgument() {
    refactoringTestHelper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  void method(Long l) {}",
            "  void method(double l) {}",
            "  {",
            "    method(9223372036854775806L);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  void method(Long l) {}",
            "  void method(double l) {}",
            "  {",
            "    method((double) 9223372036854775806L);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactors_complexArgument() {
    refactoringTestHelper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  void method(Long l) {}",
            "  void method(double l) {}",
            "  {",
            "    method(9223372036854775805L + 1L);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  void method(Long l) {}",
            "  void method(double l) {}",
            "  {",
            "    method((double) (9223372036854775805L + 1L));",
            "  }",
            "}")
        .doTest();
  }
}
