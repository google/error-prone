/*
 * Copyright 2017 The Error Prone Authors.
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
import static org.junit.Assume.assumeTrue;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link FloatingPointLiteralPrecision}Test */
@RunWith(JUnit4.class)
public class FloatingPointLiteralPrecisionTest {

  @Test
  public void positive() {
    BugCheckerRefactoringTestHelper.newInstance(FloatingPointLiteralPrecision.class, getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  double d = 1.99999999999999999999999;",
            "  float f = 1.99999999999999999999999f;",
            "}")
        .addOutputLines(
            "out/Test.java", //
            "class Test {",
            "  double d = 2.0;",
            "  float f = 2.0f;",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative() {
    CompilationTestHelper.newInstance(FloatingPointLiteralPrecision.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  double d2 = 1.0;",
            "  double d3 = 1;",
            "  double d4 = 1e6;",
            "  double d5 = 1e-3;",
            "  double d6 = 1d;",
            "  double d7 = 1_000.0;",
            "  double d8 = 0x1.0p63d;",
            "  float f2 = 1.0f;",
            "  float f3 = 1f;",
            "  float f4 = 0.88f;",
            "  float f5 = 1_000.0f;",
            "  float f6 = 0x1.0p63f;",
            "}")
        .doTest();
  }

  @Test
  public void replacementTooLong() {
    // In JDK versions before 19, String.valueOf(1e23) was 9.999999999999999E22, and the logic we're
    // testing here was introduced to avoid introducing strings like that in rewrites. JDK 19 fixes
    // https://bugs.openjdk.org/browse/JDK-4511638 (over 20 years after it was filed) so
    // we don't need the logic or its test there.
    String string1e23 = String.valueOf(1e23);
    assumeTrue(string1e23.length() > "1e23".length() * 3);

    String[] input = {
      "class Test {", //
      "  // BUG: Diagnostic contains:",
      "  double d = 1e23;",
      "}"
    };

    // Don't provide a fix if the replacement is much longer than the current literal.
    BugCheckerRefactoringTestHelper.newInstance(FloatingPointLiteralPrecision.class, getClass())
        .addInputLines("in/Test.java", input)
        .expectUnchanged()
        .doTest(TEXT_MATCH);

    // Make sure we're still emitting a warning.
    CompilationTestHelper.newInstance(FloatingPointLiteralPrecision.class, getClass())
        .addSourceLines("Test.java", input)
        .doTest();
  }
}
