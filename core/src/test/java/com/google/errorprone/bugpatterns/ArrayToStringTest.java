/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

/** {@link ArrayToString}Test */
@RunWith(JUnit4.class)
public class ArrayToStringTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ArrayToString.class, getClass());
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.addSourceFile("ArrayToStringPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.addSourceFile("ArrayToStringNegativeCases.java").doTest();
  }

  @Test
  public void stringConcat() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int[] xs) {",
            "    // BUG: Diagnostic contains: (\"\" + Arrays.toString(xs));",
            "    System.err.println(\"\" + xs);",
            "    String s = \"\";",
            "    // BUG: Diagnostic contains: s += Arrays.toString(xs);",
            "    s += xs;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void printString() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int[] g() { return null; }",
            "  void f(int[] xs) {",
            "    // BUG: Diagnostic contains: println(Arrays.toString(xs))",
            "    System.err.println(xs);",
            "    // BUG: Diagnostic contains: println(Arrays.toString(xs))",
            "    System.err.println(String.valueOf(xs));",
            "    // BUG: Diagnostic contains: println(Arrays.toString(g()))",
            "    System.err.println(String.valueOf(g()));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativePrintString() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(char[] xs) {",
            "    System.err.println(String.valueOf(xs));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stringBuilder() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int[] xs) {",
            "    // BUG: Diagnostic contains: append(Arrays.toString(xs))",
            "    new StringBuilder().append(xs);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCompoundAssignment() throws Exception {
    compilationHelper.addSourceFile("ArrayToStringCompoundAssignmentPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCompoundAssignment() throws Exception {
    compilationHelper.addSourceFile("ArrayToStringCompoundAssignmentNegativeCases.java").doTest();
  }

  @Test
  public void testPositiveConcat() throws Exception {
    compilationHelper.addSourceFile("ArrayStringConcatenationPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeConcat() throws Exception {
    compilationHelper.addSourceFile("ArrayStringConcatenationNegativeCases.java").doTest();
  }
}
