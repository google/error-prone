/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

/** {@link MethodCanBeStatic}Test */
@RunWith(JUnit4.class)
public class MethodCanBeStaticTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(MethodCanBeStatic.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: private static int add(",
            "  private int add(int x, int y) {",
            "    return x + y;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int base = 0;",
            "  private int f(int x, int y) {",
            "    return base++ + x + y;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveTypeParameter() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test<T> {",
            "  // BUG: Diagnostic contains: private static <T> T f(",
            "  private <T> T f(int x, int y) {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeTypeParameter() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test<T> {",
            "  private T f(int x, int y) {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeConstructor() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  Test() {",
            "}",
            "}")
        .doTest();
  }

  @Test
  public void negativePublic() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public String toString() {",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeOverride() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @Override",
            "  public String toString() {",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeMethodCall() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int x;",
            "  int f() {",
            "    return x++;",
            "  }",
            "  private int g() {",
            "    return f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nativeMethod() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  private native int f();",
            "}")
        .doTest();
  }
}
