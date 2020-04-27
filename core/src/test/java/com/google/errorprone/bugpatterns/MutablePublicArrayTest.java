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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link MutablePublicArray}. */
@RunWith(JUnit4.class)
public class MutablePublicArrayTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MutablePublicArray.class, getClass());

  @Test
  public void publicStaticFinalPrimitive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: MutablePublicArray",
            "  public static final int[] array = new int[10];",
            "}")
        .doTest();
  }

  @Test
  public void publicStaticFinalInlineInitializer() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: MutablePublicArray",
            "  public static final String[] array = {\"foo\", \"bar\"};",
            "}")
        .doTest();
  }

  @Test
  public void publicStaticFinalObject() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: MutablePublicArray",
            "  public static final Test[] array = new Test[10];",
            "}")
        .doTest();
  }

  @Test
  public void publicStaticFinalObjectMultiDimension() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: MutablePublicArray",
            "  public static final Test[][] array = new Test[10][10];",
            "}")
        .doTest();
  }

  @Test
  public void privateStaticFinal_Negative() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  private static final int[] array = new int[10];",
            "}")
        .doTest();
  }

  @Test
  public void privateStaticFinalEmptyInlineInitializer_Negative() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  public static final String[] array = {};",
            "}")
        .doTest();
  }

  @Test
  public void privateFinal_Negative() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  private final int[] array = new int[10];",
            "}")
        .doTest();
  }

  @Test
  public void staticFinal_Negative() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  static final int[] array = new int[10];",
            "}")
        .doTest();
  }

  @Test
  public void zeroSizeOneDimensionArray_Negative() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  public static final int[] array = new int[0];",
            "}")
        .doTest();
  }

  @Test
  public void zeroSizeMultiDimensionArray_Negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public static final int[][] array = new int[0][0];",
            "}")
        .doTest();
  }

  @Test
  public void negative_datapoints() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.experimental.theories.DataPoints;",
            "class Test {",
            "  @DataPoints",
            "  public static final int[] array = new int[10];",
            "}")
        .doTest();
  }
}
