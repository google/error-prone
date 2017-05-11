/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link JUnit3FloatingPointComparisonWithoutDelta}.
 *
 * @author mwacker@google.com (Mike Wacker)
 */
@RunWith(JUnit4.class)
public class JUnit3FloatingPointComparisonWithoutDeltaTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void createCompilerWithErrorProneCheck() {
    compilationHelper =
        CompilationTestHelper.newInstance(
            JUnit3FloatingPointComparisonWithoutDelta.class, getClass());
  }

  @Test
  public void match_TwoPrimitiveDoubles() {
    checkAssertEquals("1.0, 1.0", true);
  }

  @Test
  public void match_PrimitiveAndReferenceDouble() {
    checkAssertEquals("1.0, (Double) 1.0", true);
  }

  @Test
  public void match_ReferenceAndPrimitiveDouble() {
    checkAssertEquals("(Double) 1.0, 1.0", true);
  }

  @Test
  public void noMatch_TwoReferenceDoubles() {
    checkAssertEquals("(Double) 1.0, (Double) 1.0", false);
  }

  @Test
  public void match_TwoPrimitiveDoublesWithMessage() {
    checkAssertEquals("\"message\", 1.0, 1.0", true);
  }

  @Test
  public void noMatch_DeltaArgumentUsed() {
    checkAssertEquals("1.0, 1.0, 0.0", false);
  }

  @Test
  public void noMatch_TwoPrimitiveInts() {
    checkAssertEquals("1, 1", false);
  }

  @Test
  public void noMatch_TwoStrings() {
    checkAssertEquals("\"abc\", \"abc\"", false);
  }

  @Test
  public void noMatch_PrimitiveDoubleAndString() {
    checkAssertEquals("1.0, \"abc\"", false);
  }

  @Test
  public void match_TwoPrimitiveFloats() {
    checkAssertEquals("1.0f, 1.0f", true);
  }

  @Test
  public void match_ReferenceAndPrimitiveFloat() {
    checkAssertEquals("(Float) 1.0f, 1.0f", true);
  }

  @Test
  public void noMatch_TwoReferenceFloats() {
    checkAssertEquals("(Float) 1.0f, (Float) 1.0f", false);
  }

  @Test
  public void match_PrimitiveFloatAndPrimitiveDouble() {
    checkAssertEquals("1.0f, 1.0", true);
  }

  @Test
  public void match_PrimitiveFloatAndReferenceDouble() {
    checkAssertEquals("1.0f, (Double) 1.0", true);
  }

  @Test
  public void match_PrimitiveIntAndPrimitiveDouble() {
    checkAssertEquals("1, 1.0", true);
  }

  @Test
  public void match_PrimitiveDoubleAndReferenceInteger() {
    checkAssertEquals("1.0, (Integer) 1", true);
  }

  @Test
  public void match_PrimitiveCharAndPrimitiveDouble() {
    checkAssertEquals("'a', 1.0", true);
  }

  @Test
  public void noMatch_notAssertEquals() {
    checkTest("    assertSame(1.0, 1.0);", false);
  }

  @Test
  public void noMatch_notTestCase() {
    compilationHelper
        .addSourceLines(
            "SampleClass.java",
            "public class SampleClass {",
            "  public void assertEquals(double d1, double d2) {}",
            "  public void f() {",
            "    assertEquals(1.0, 1.0);",
            "  }",
            "}")
        .doTest();
  }

  private void checkAssertEquals(String assertEqualsArgumentsText, boolean matchExpected) {
    String assertEqualsLine = String.format("    assertEquals(%s);", assertEqualsArgumentsText);
    checkTest(assertEqualsLine, matchExpected);
  }

  private void checkTest(String methodInvocationLine, boolean matchExpected) {
    String diagnosticLine = matchExpected ? "    // BUG: Diagnostic contains:" : "";
    compilationHelper
        .addSourceLines(
            "SampleTest.java",
            "import junit.framework.TestCase;",
            "public class SampleTest extends TestCase {",
            "  public void testComparison() {",
            diagnosticLine,
            methodInvocationLine,
            "  }",
            "}")
        .doTest();
  }
}
