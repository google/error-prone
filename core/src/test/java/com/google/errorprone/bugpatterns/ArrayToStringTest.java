/*
 * Copyright 2012 The Error Prone Authors.
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

/** {@link ArrayToString}Test */
@RunWith(JUnit4.class)
public class ArrayToStringTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ArrayToString.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(ArrayToString.class, getClass());

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("ArrayToStringPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("ArrayToStringNegativeCases.java").doTest();
  }

  @Test
  public void stringConcat() {
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
  public void printString() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  int[] g() { return null; }",
            "  void f(int[] xs) {",
            "    System.err.println(xs);",
            "    System.err.println(String.valueOf(xs));",
            "    System.err.println(String.valueOf(g()));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Arrays;",
            "class Test {",
            "  int[] g() { return null; }",
            "  void f(int[] xs) {",
            "    System.err.println(Arrays.toString(xs));",
            "    System.err.println(Arrays.toString(xs));",
            "    System.err.println(Arrays.toString(g()));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativePrintString() {
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
  public void stringBuilder() {
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

  /**
   * Don't complain about {@code @FormatMethod}s; there's a chance they're handling arrays
   * correctly.
   */
  @Test
  public void customFormatMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.FormatMethod;",
            "class Test {",
            "  private void test(Object[] arr) {",
            "    format(\"%s %s\", arr, 2);",
            "  }",
            "  @FormatMethod",
            "  String format(String format, Object... args) {",
            "    return String.format(format, args);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReturningArray() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private void test() {",
            "    // BUG: Diagnostic contains:",
            "    String.format(\"%s %s\", arr(), 1);",
            "  }",
            "  Object[] arr() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void throwableToString() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test(Exception e) {",
            "    // BUG: Diagnostic contains: Throwables.getStackTraceAsString(e)",
            "    String.format(\"%s, %s\", 1, e.getStackTrace());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCompoundAssignment() {
    compilationHelper.addSourceFile("ArrayToStringCompoundAssignmentPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCompoundAssignment() {
    compilationHelper.addSourceFile("ArrayToStringCompoundAssignmentNegativeCases.java").doTest();
  }

  @Test
  public void testPositiveConcat() {
    compilationHelper.addSourceFile("ArrayToStringConcatenationPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeConcat() {
    compilationHelper.addSourceFile("ArrayToStringConcatenationNegativeCases.java").doTest();
  }
}
