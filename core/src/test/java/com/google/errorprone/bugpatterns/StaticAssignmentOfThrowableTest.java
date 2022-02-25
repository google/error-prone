/*
 * Copyright 2022 The Error Prone Authors.
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

/** Tests for {@link StaticAssignmentOfThrowable}. */
@RunWith(JUnit4.class)
public final class StaticAssignmentOfThrowableTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(StaticAssignmentOfThrowable.class, getClass());

  @Test
  public void staticWithThrowableInMethod_error() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static Throwable foo;",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo() { ",
            "    // BUG: Diagnostic contains: [StaticAssignmentOfThrowable]",
            "    foo = new NullPointerException(\"assign in method\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticWithThrowableDuringInitialization_error() {

    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            " // BUG: Diagnostic contains: [StaticAssignmentOfThrowable]",
            "  static Throwable foo = new NullPointerException(\"message\");",
            "  public Test(int foo) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticWithThrowableDuringInitializationFromMethod_error() {

    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: [StaticAssignmentOfThrowable]",
            "  static Throwable foo = bar(); ",
            "  public Test(int foo) {",
            "  } ",
            " ",
            "  private static Throwable bar() { ",
            "    return new NullPointerException(\"initialized with return value\"); ",
            "  } ",
            "}")
        .doTest();
  }

  @Test
  public void dynamicWithThrowableDuringInitializationFromMethod_noMatch() {

    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  Throwable foo = bar(); ",
            "  public Test(int foo) {",
            "  } ",
            " ",
            "  private static Throwable bar() { ",
            "    return new NullPointerException(\"initialized with return value\"); ",
            "  } ",
            "}")
        .doTest();
  }

  @Test
  public void staticWithThrowableDuringConstructor_noMatch() {

    // Handling this scenario delegated to StaticAssignmentInConstructor
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static Throwable foo;",
            "  public Test(int bar) {",
            "    foo = new NullPointerException(Integer.toString(bar));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticWithNonThrowableFromMethod_noMatch() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static int foo;",
            "  public Test(int foo) {",
            "  }",
            "  private void bar() { ",
            "    this.foo = 5;",
            "  } ",
            "}")
        .doTest();
  }

  @Test
  public void staticWithNonThrowableFromDeclaration_noMatch() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private static final String RULE = \"allow this\";",
            "  public Test(int foo) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void dynamic_noMatch() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  Throwable foo;",
            "  public Test(int foo) {",
            "    this.foo = new RuntimeException(\"odd but not an error here\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticWithThrowableInLambdaInMethod_error() {

    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static Throwable foo;",
            "  public Test(int a) {",
            "  } ",
            " void foo(int a) { ",
            "    java.util.Arrays.asList().stream().map(x -> { ",
            "      // BUG: Diagnostic contains: [StaticAssignmentOfThrowable]",
            "      foo = new NullPointerException(\"assign\"); ",
            "      return a; }) ",
            "    .count();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticWithThrowableInLambdaInLambdaInMethod_error() {

    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static Throwable foo;",
            "  public Test(int a) {",
            "  } ",
            " void bar(int a) { ",
            "    java.util.Arrays.asList().stream().map(x -> { ",
            "      java.util.Arrays.asList().stream().map(y -> { ",
            "        // BUG: Diagnostic contains: [StaticAssignmentOfThrowable]",
            "        foo = new NullPointerException(\"inner assign\"); return y;}",
            "      ).count(); ",
            "      // BUG: Diagnostic contains: [StaticAssignmentOfThrowable]",
            "      foo = new NullPointerException(\"outer assign\"); ",
            "      return a; }) ",
            "    .count();",
            "  }",
            "}")
        .doTest();
  }
}
