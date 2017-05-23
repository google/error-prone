/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@code NamedParameterChecker} */
@RunWith(JUnit4.class)
public class NamedParameterCheckerTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(NamedParameterChecker.class, getClass());
  }

  @Test
  public void namedParametersChecker_ignoresCall_withNoComments() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object arg1, Object arg2) {",
            "    target(arg1, arg2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_findsError_withOneBadComment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object arg1, Object arg2) {",
            "    // BUG: Diagnostic contains: target(arg2, /*param2=*/arg1)",
            "    target(/*param2=*/arg1, arg2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_suggestsSwap_withSwappedArgs() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object arg1, Object arg2) {",
            "    // BUG: Diagnostic contains: target(/*param1=*/arg1, /*param2=*/arg2)",
            "    target(/*param2=*/arg2, /*param1=*/arg1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_suggestsSwap_withOneCommentedSwappedArgs() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object arg1, Object arg2) {",
            "    // BUG: Diagnostic contains: target(arg1, /*param2=*/arg2)",
            "    target(/*param2=*/arg2, arg1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_suggestsComments_onRequiredNamesMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.RequiresNamedParameters;",
            "abstract class Test {",
            "  @RequiresNamedParameters",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object arg1, Object arg2) {",
            "    // BUG: Diagnostic contains: target(/*param1=*/arg1, /*param2=*/arg2)",
            "    target(arg1, arg2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_ignoresCall_onRequiredNamesMethodWithComments() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.RequiresNamedParameters;",
            "abstract class Test {",
            "  @RequiresNamedParameters",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object arg1, Object arg2) {",
            "    target(/*param1=*/arg1, /*param2=*/arg2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_suggestsCommentsAndSwaps_onRequiredNamesMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.RequiresNamedParameters;",
            "abstract class Test {",
            "  @RequiresNamedParameters",
            "  abstract void target(Object p1, Object p2, Object p3);",
            "  void test(Object arg1, Object arg2, Object arg3) {",
            "    // BUG: Diagnostic contains: target(/*p1=*/arg1, /*p2=*/arg2, /*p3=*/arg3)",
            "    target(/*p3=*/arg3, arg2, /*p1=*/arg1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_doesNotMatchComment_withSpacesAndExtraText() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  void test(Object arg) {",
            "    // BUG: Diagnostic contains: target(/*param=*/arg)",
            "    target(/*note param = */arg);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_matchesComment_withChainedMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract Test getTest(Object param);",
            "  abstract void target(Object param2);",
            "  void test(Object arg, Object arg2) {",
            "    getTest(/*param=*/arg).target(arg2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_suggestsChangeComment_whenNoMatchingNames() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object arg1, Object arg2) {",
            "    // BUG: Diagnostic contains: target(/*param1=*/arg1, arg2)",
            "    target(/*parm1=*/arg1, arg2);",
            "  }",
            "}")
        .doTest();
  }
}
