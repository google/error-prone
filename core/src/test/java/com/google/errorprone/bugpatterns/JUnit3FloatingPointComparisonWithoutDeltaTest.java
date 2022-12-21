/*
 * Copyright 2016 The Error Prone Authors.
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

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(
          JUnit3FloatingPointComparisonWithoutDelta.class, getClass());

  @Test
  public void match_twoPrimitiveDoubles() {
    checkAssertEquals("1.0, 1.0", true);
  }

  @Test
  public void match_primitiveAndReferenceDouble() {
    checkAssertEquals("1.0, (Double) 1.0", true);
  }

  @Test
  public void match_referenceAndPrimitiveDouble() {
    checkAssertEquals("(Double) 1.0, 1.0", true);
  }

  @Test
  public void noMatch_twoReferenceDoubles() {
    checkAssertEquals("(Double) 1.0, (Double) 1.0", false);
  }

  @Test
  public void match_twoPrimitiveDoublesWithMessage() {
    checkAssertEquals("\"message\", 1.0, 1.0", true);
  }

  @Test
  public void noMatch_deltaArgumentUsed() {
    checkAssertEquals("1.0, 1.0, 0.0", false);
  }

  @Test
  public void noMatch_twoPrimitiveInts() {
    checkAssertEquals("1, 1", false);
  }

  @Test
  public void noMatch_twoStrings() {
    checkAssertEquals("\"abc\", \"abc\"", false);
  }

  @Test
  public void noMatch_primitiveDoubleAndString() {
    checkAssertEquals("1.0, \"abc\"", false);
  }

  @Test
  public void match_twoPrimitiveFloats() {
    checkAssertEquals("1.0f, 1.0f", true);
  }

  @Test
  public void match_referenceAndPrimitiveFloat() {
    checkAssertEquals("(Float) 1.0f, 1.0f", true);
  }

  @Test
  public void noMatch_twoReferenceFloats() {
    checkAssertEquals("(Float) 1.0f, (Float) 1.0f", false);
  }

  @Test
  public void match_primitiveFloatAndPrimitiveDouble() {
    checkAssertEquals("1.0f, 1.0", true);
  }

  @Test
  public void match_primitiveFloatAndReferenceDouble() {
    checkAssertEquals("1.0f, (Double) 1.0", true);
  }

  @Test
  public void match_primitiveIntAndPrimitiveDouble() {
    checkAssertEquals("1, 1.0", true);
  }

  @Test
  public void match_primitiveDoubleAndReferenceInteger() {
    checkAssertEquals("1.0, (Integer) 1", true);
  }

  @Test
  public void match_primitiveCharAndPrimitiveDouble() {
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
