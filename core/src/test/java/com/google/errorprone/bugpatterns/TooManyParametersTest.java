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

import static com.google.errorprone.bugpatterns.TooManyParameters.TOO_MANY_PARAMETERS_FLAG_NAME;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.ErrorProneFlags;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link TooManyParameters}. */
@RunWith(JUnit4.class)
public class TooManyParametersTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setup() {
    compilationHelper = CompilationTestHelper.newInstance(TooManyParameters.class, getClass());
  }

  @Test
  public void testZeroLimit() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TooManyParameters(
                ErrorProneFlags.builder().putFlag(TOO_MANY_PARAMETERS_FLAG_NAME, "0").build()));
  }

  @Test
  public void testNegativeLimit() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TooManyParameters(
                ErrorProneFlags.builder().putFlag(TOO_MANY_PARAMETERS_FLAG_NAME, "-1").build()));
  }

  @Test
  public void constructor() {
    compilationHelper
        .setArgs(ImmutableList.of("-XepOpt:" + TOO_MANY_PARAMETERS_FLAG_NAME + "=3"))
        .addSourceLines(
            "ConstructorTest.java",
            "public class ConstructorTest {",
            "  public ConstructorTest() {",
            "  }",
            "  public ConstructorTest(int a) {",
            "  }",
            "  public ConstructorTest(int a, int b) {",
            "  }",
            "  public ConstructorTest(int a, int b, int c) {",
            "  }",
            "  // BUG: Diagnostic contains: 4 parameters",
            "  public ConstructorTest(int a, int b, int c, int d) {",
            "  }",
            "  // BUG: Diagnostic contains: 5 parameters",
            "  public ConstructorTest(int a, int b, int c, int d, int e) {",
            "  }",
            "  // BUG: Diagnostic contains: 6 parameters",
            "  public ConstructorTest(int a, int b, int c, int d, int e, int f) {",
            "  }",
            "  private ConstructorTest(int a, int b, int c, int d, int e, int f, int g) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_withAtInject() {
    compilationHelper
        .setArgs(ImmutableList.of("-XepOpt:" + TOO_MANY_PARAMETERS_FLAG_NAME + "=3"))
        .addSourceLines(
            "ConstructorTest.java",
            "import javax.inject.Inject;",
            "public class ConstructorTest {",
            "  public ConstructorTest() {",
            "  }",
            "  public ConstructorTest(int a) {",
            "  }",
            "  public ConstructorTest(int a, int b) {",
            "  }",
            "  public ConstructorTest(int a, int b, int c) {",
            "  }",
            "  @Inject public ConstructorTest(int a, int b, int c, int d) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void method() {
    compilationHelper
        .setArgs(ImmutableList.of("-XepOpt:" + TOO_MANY_PARAMETERS_FLAG_NAME + "=3"))
        .addSourceLines(
            "MethodTest.java",
            "public class MethodTest {",
            "  public void foo() {",
            "  }",
            "  public void foo(int a) {",
            "  }",
            "  public void foo(int a, int b) {",
            "  }",
            "  public void foo(int a, int b, int c) {",
            "  }",
            "  // BUG: Diagnostic contains: 4 parameters",
            "  public void foo(int a, int b, int c, int d) {",
            "  }",
            "  // BUG: Diagnostic contains: 5 parameters",
            "  public void foo(int a, int b, int c, int d, int e) {",
            "  }",
            "  // BUG: Diagnostic contains: 6 parameters",
            "  public void foo(int a, int b, int c, int d, int e, int f) {",
            "  }",
            "  private void foo(int a, int b, int c, int d, int e, int f, int g) {",
            "  }",
            "}")
        .doTest();
  }
}
