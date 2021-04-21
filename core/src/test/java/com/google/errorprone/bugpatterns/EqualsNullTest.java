/*
 * Copyright 2021 The Error Prone Authors.
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

/** Unit tests for {@link EqualsNull} check. */
@RunWith(JUnit4.class)
public class EqualsNullTest {

  private static final String EXPECTED_BUG_COMMENT =
      "// BUG: Diagnostic contains: x.equals(null) should return false";

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(EqualsNull.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(EqualsNull.class, getClass());

  @Test
  public void negativeSimpleCase() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  boolean m(Object x, Object y) {",
            "    return x.equals(y);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeJUnit4TestClass() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "class Test {",
            "  boolean m(Object x) {",
            "    return x.equals(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeTestNgTestClass() {
    compilationTestHelper
        .addSourceLines(
            "org/testng/annotations/Test.java",
            "package org.testng.annotations;",
            "public @interface Test {",
            "}")
        .addSourceLines(
            "MyTest.java",
            "import org.testng.annotations.Test;",
            "@Test",
            "class MyTest {",
            "  boolean m(Object x) {",
            "    return x.equals(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeAssertClass() {
    compilationTestHelper
        .addSourceLines(
            "AssertHelper.java",
            "import org.junit.Assert;",
            "class AssertHelper extends Assert {",
            "  public static void myAssert(Object x) {",
            "    x.equals(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeEnclosedByJUnitAssert() {
    compilationTestHelper
        .addSourceLines(
            "TestHelper.java",
            "import static org.junit.Assert.assertFalse;",
            "class TestHelper {",
            "  public static void myAssert(Object x) {",
            "    assertFalse(x.equals(null));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeEnclosedByTruthAssert() {
    compilationTestHelper
        .addSourceLines(
            "TestHelper.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "class TestHelper {",
            "  public static void myAssert(Object x) {",
            "    assertThat(x.equals(null)).isFalse();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveSimpleCase() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  boolean m(Object x) {",
            EXPECTED_BUG_COMMENT,
            "    return x.equals(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveReturnObjectEqualsNullFix() {
    refactoringTestHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  boolean m(Object x) {",
            "    return x.equals(null);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  boolean m(Object x) {",
            "    return x == null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveReturnObjectNotEqualsNullFix() {
    refactoringTestHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  boolean m(Object x) {",
            "    return !x.equals(null);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  boolean m(Object x) {",
            "    return x != null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveIfObjectEqualsNullFix() {
    refactoringTestHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void m(Object x) {",
            "    if (x.equals(null)) {",
            "       return;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void m(Object x) {",
            "    if (x == null) {",
            "       return;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveMethodReturnValueNotEqualsNullFix() {
    refactoringTestHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  boolean m(Object x) {",
            "    return !x.toString().equals(null);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  boolean m(Object x) {",
            "    return x.toString() != null;",
            "  }",
            "}")
        .doTest();
  }
}
