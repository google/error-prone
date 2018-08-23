/*
 * Copyright 2015 The Error Prone Authors.
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

/** {@link NonFinalCompileTimeConstant}Test */
@RunWith(JUnit4.class)
public class NonFinalCompileTimeConstantTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(NonFinalCompileTimeConstant.class, getClass());
  }

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  public void f(@CompileTimeConstant Object x) {",
            "    x = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveTwoParams() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  public void f(@CompileTimeConstant Object x, @CompileTimeConstant Object y) {",
            "    x = y = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveOneOfTwoParams() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class Test {",
            "  public void f(",
            "      @CompileTimeConstant Object x,",
            "      // BUG: Diagnostic contains:",
            "      @CompileTimeConstant Object y) {",
            "    y = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class Test {",
            "  public void f(final @CompileTimeConstant Object x) {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeEffectivelyFinal() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class Test {",
            "  public void f(@CompileTimeConstant Object x) {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeInterface() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public interface Test {",
            "  public void f(@CompileTimeConstant Object x);",
            "}")
        .doTest();
  }
}
