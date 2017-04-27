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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.bugpatterns.BugChecker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for AssertEqualsArgumentOrderChecker */
@RunWith(JUnit4.class)
public class AssertEqualsArgumentOrderCheckerTest {

  private CompilationTestHelper compilationHelper;

  /**
   * A {@link BugChecker} which runs the AssertEqualsArgumentOrderChecker which looks for
   * assertEquals methods declared in a class Test
   */
  @BugPattern(
    name = "AssertEqualsArgumentOrderCheckerInTest",
    category = Category.ONE_OFF,
    severity = SeverityLevel.ERROR,
    summary = "Run AssertEqualsArgumentOrderChecker looking for methods declared in class Test"
  )
  public static class AssertEqualsArgumentOrderCheckerInTest
      extends AssertEqualsArgumentOrderChecker {

    public AssertEqualsArgumentOrderCheckerInTest() {
      super(ImmutableList.of("Test"));
    }
  }

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(AssertEqualsArgumentOrderCheckerInTest.class, getClass());
  }

  @Test
  public void assertEqualsCheck_makesNoSuggestion_withOrderExpectedActual() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  static void assertEquals(Object expected, Object actual) {};",
            "  void test(Object expected, Object actual) {",
            "    assertEquals(expected, actual);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertEqualsCheck_makesNoSuggestion_withOrderExpectedActualAndMessage()
      throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  static void assertEquals(String message, Object expected, Object actual) {};",
            "  void test(Object expected, Object actual) {",
            "    assertEquals(\"\", expected, actual);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertEqualsCheck_swapsArguments_withOrderActualExpected() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  static void assertEquals(Object expected, Object actual) {};",
            "  void test(Object expected, Object actual) {",
            "    // BUG: Diagnostic contains: assertEquals(expected, actual)",
            "    assertEquals(actual, expected);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertEqualsCheck_swapsArguments_withOrderActualExpectedAndMessage()
      throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  static void assertEquals(String message, Object expected, Object actual) {};",
            "  void test(Object expected, Object actual) {",
            "    // BUG: Diagnostic contains: assertEquals(\"\", expected, actual)",
            "    assertEquals(\"\", actual, expected);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertEqualsCheck_swapsArguments_withOnlyExpectedAsPrefix() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  static void assertEquals(Object expected, Object actual) {};",
            "  abstract Object get();",
            "  void test(Object expectedValue) {",
            "    // BUG: Diagnostic contains: assertEquals(expectedValue, get())",
            "    assertEquals(get(), expectedValue);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertEqualsCheck_swapsArguments_withLiteralForActual() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  static void assertEquals(Object expected, Object actual) {};",
            "  void test(Object other) {",
            "    // BUG: Diagnostic contains: assertEquals(1, other)",
            "    assertEquals(other, 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertEqualsCheck_doesntSwap_withLiteralForExpected() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  static void assertEquals(String mesasge, Object expected, Object actual) {};",
            "  void test(Object other) {",
            "    assertEquals(\"\",1, other);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertEqualsCheck_makeNoChange_withLiteralForBoth() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  static void assertEquals(Object expected, Object actual) {};",
            "  void test() {",
            "    assertEquals(2, 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertEqualsCheck_makeNoChange_ifSwapCreatesDuplicateCall() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  static void assertEquals(Object expected, Object actual) {};",
            "  void test(Object expected, Object actual) {",
            "    assertEquals(expected, actual);",
            "    assertEquals(actual, expected);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertEqualsCheck_makesNoChange_withNothingMatching() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  static void assertEquals(Object expected, Object actual) {};",
            "  void test(Object other1, Object other2) {",
            "    assertEquals(other1, other2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertEqualsCheck_makesNoChange_whenArgumentExtendsThrowable() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  static void assertEquals(Object expected, Object actual) {};",
            "  void test(Exception exception) {",
            "    try {",
            "      throw exception;",
            "    } catch (Exception expected) {",
            "      assertEquals(exception,expected);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertEqualsCheck_makesNoChange_whenArgumentIsEnumMember() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  static void assertEquals(Object expected, Object actual) {};",
            "  enum MyEnum {",
            "    VALUE",
            "  }",
            "  void test(MyEnum expected) {",
            "    assertEquals(MyEnum.VALUE, expected);",
            "  }",
            "}")
        .doTest();
  }
}
