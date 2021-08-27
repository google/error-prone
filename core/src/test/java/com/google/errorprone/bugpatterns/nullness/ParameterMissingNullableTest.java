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

package com.google.errorprone.bugpatterns.nullness;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ParameterMissingNullable}Test */
@RunWith(JUnit4.class)
public class ParameterMissingNullableTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(ParameterMissingNullable.class, getClass());

  @Test
  public void testPositiveIf() {
    helper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer i) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    if (i == null) {",
            "      i = 0;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveIfWithUnrelatedThrow() {
    helper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(boolean b, Integer i) {",
            "    if (b) {",
            "      // BUG: Diagnostic contains: @Nullable",
            "      int val = i == null ? 0 : i;",
            "      if (val < 0) {",
            "        throw new RuntimeException();",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveDespiteWhileLoop() {
    helper
        .addSourceLines(
            "Foo.java",
            "import static com.google.common.base.Preconditions.checkArgument;",
            "class Foo {",
            "  void foo(Object o) {",
            "    while (true)",
            "      checkArgument(o != null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveTernary() {
    helper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  int i;",
            "  void foo(Integer i) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    this.i = i == null ? 0 : i;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeAlreadyAnnotated() {
    helper
        .addSourceLines(
            "Foo.java",
            "import javax.annotation.Nullable;",
            "class Foo {",
            "  void foo(@Nullable Integer i) {",
            "    if (i == null) {",
            "      i = 0;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativePreconditionCheckMethod() {
    helper
        .addSourceLines(
            "Foo.java",
            "import static com.google.common.base.Preconditions.checkArgument;",
            "class Foo {",
            "  void foo(Integer i) {",
            "    checkArgument(i != null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeOtherCheckMethod() {
    helper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void assertNot(boolean b) {}",
            "  void foo(Integer i) {",
            "    assertNot(i == null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeAssert() {
    helper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer i) {",
            "    assert (i != null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCheckNotAgainstNull() {
    helper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer i) {",
            "    if (i == 7) {",
            "      i = 0;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCheckOfNonParameter() {
    helper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer i) {",
            "    Integer j = 7;",
            "    if (j == null) {",
            "      i = 0;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeThrow() {
    helper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer i) {",
            "    if (i == null) {",
            "      throw something();",
            "    }",
            "  }",
            "  RuntimeException something() {",
            "    return new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCreateException() {
    helper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo(Integer i) {",
            "    if (i == null) {",
            "      throwIt(new RuntimeException());",
            "    }",
            "  }",
            "  void throwIt(RuntimeException x) {",
            "    throw x;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeLambdaParameter() {
    helper
        .addSourceLines(
            "Foo.java",
            "interface Foo {",
            "  Foo FOO = o -> o == null ? 0 : o;",
            "  int toInt(Integer o);",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeDoWhileLoop() {
    helper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  Foo next;",
            "  void foo(Foo foo) {",
            "    do {",
            "      foo = foo.next;",
            "    } while (foo != null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeWhileLoop() {
    /*
     * It would be safe to annotate this parameter as @Nullable, but it's somewhat unclear whether
     * people would prefer that in most cases. We could consider adding @Nullable if people would
     * find it useful.
     */
    helper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  Foo next;",
            "  void foo(Foo foo) {",
            "    while (foo != null) {",
            "      foo = foo.next;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeForLoop() {
    // Similar to testNegativeWhileLoop, @Nullable would be defensible here.
    helper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  Foo next;",
            "  void foo(Foo foo) {",
            "    for (; foo != null; foo = foo.next) {}",
            "  }",
            "}")
        .doTest();
  }
}
