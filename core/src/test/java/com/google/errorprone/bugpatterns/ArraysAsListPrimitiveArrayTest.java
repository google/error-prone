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

/** {@link ArraysAsListPrimitiveArray}Test */
@RunWith(JUnit4.class)
public class ArraysAsListPrimitiveArrayTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ArraysAsListPrimitiveArray.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Arrays;",
            "class Test {",
            "void f() {",
            "  // BUG: Diagnostic contains: Booleans.asList(new boolean[0]);",
            "  Arrays.asList(new boolean[0]);",
            "  // BUG: Diagnostic contains: Bytes.asList(new byte[0]);",
            "  Arrays.asList(new byte[0]);",
            "  // BUG: Diagnostic contains: Shorts.asList(new short[0]);",
            "  Arrays.asList(new short[0]);",
            "  // BUG: Diagnostic contains: Chars.asList(new char[0]);",
            "  Arrays.asList(new char[0]);",
            "  // BUG: Diagnostic contains: Ints.asList(new int[0]);",
            "  Arrays.asList(new int[0]);",
            "  // BUG: Diagnostic contains: Longs.asList(new long[0]);",
            "  Arrays.asList(new long[0]);",
            "  // BUG: Diagnostic contains: Doubles.asList(new double[0]);",
            "  Arrays.asList(new double[0]);",
            "  // BUG: Diagnostic contains: Floats.asList(new float[0]);",
            "  Arrays.asList(new float[0]);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Arrays;",
            "class Test {",
            "void f() {",
            "  Arrays.asList(new Object());",
            "  Arrays.asList(new Object[0]);",
            "  Arrays.asList(new Integer[0]);",
            "  }",
            "}")
        .doTest();
  }
}
