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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link AlreadyChecked}. */
@RunWith(JUnit4.class)
public final class AlreadyCheckedTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(AlreadyChecked.class, getClass());

  @Test
  public void elseChecksSameVariable() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void test(boolean a) {",
            "    if (a) {",
            "    // BUG: Diagnostic contains: false",
            "    } else if (a) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void sameVariableCheckedTwice() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void test(boolean a) {",
            "    if (a) {",
            "      // BUG: Diagnostic contains: true",
            "      if (a) {}",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void sameVariableCheckedThrice() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void test(boolean a) {",
            "    if (a) {",
            "      // BUG: Diagnostic contains: true",
            "      if (a) {}",
            "      // BUG: Diagnostic contains: true",
            "      if (a) {}",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void sameVariableCheckedTwice_negated() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void test(boolean a) {",
            "    if (a) {",
            "      // BUG: Diagnostic contains: false",
            "      if (!a) {}",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void sameVariableCheckedTwice_atTopLevel() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void test(boolean a) {",
            "    if (a) {}",
            "    if (a) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void sameVariableCheckedTwice_asPartOfAnd() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void test(boolean a, boolean b, boolean c) {",
            "    if (a && b) {",
            "      // BUG: Diagnostic contains: true",
            "      if (a && c) {}",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void sameVariableCheckedTwice_butOuterIfNotSimple() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void test(boolean a, boolean b, boolean c) {",
            "    if ((a && b) || b) {",
            "      if (a && c) {}",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void complexExpression() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void test(boolean a, boolean b, boolean c) {",
            "    if (!a || (b && c)) {",
            "    } else if (b) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void notFinal_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void test(boolean a) {",
            "    a = true;",
            "    if (a) {",
            "    } else if (a) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ternaryWithinIf() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public int test(boolean a) {",
            "    if (a) {",
            "      // BUG: Diagnostic contains: true",
            "      return a ? 1 : 2;",
            "    }",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void equalsCheckedTwice() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public int test(String a) {",
            "    if (a.equals(\"a\")) {",
            "      // BUG: Diagnostic contains: true",
            "      return a.equals(\"a\") ? 1 : 2;",
            "    }",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void equalsCheckedTwice_comparedToDifferentConstant() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public int test(String a) {",
            "    if (a.equals(\"b\")) {",
            "      return a.equals(\"a\") ? 1 : 2;",
            "    }",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void comparedUsingBinaryEquals() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public int test(int a, int b) {",
            "    if (a == 1) {",
            "      if (a == b) {",
            "        return 3;",
            "      }",
            "      // BUG: Diagnostic contains:",
            "      return a != 1 ? 1 : 2;",
            "    }",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void durationsComparedUsingFactoryMethods() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  public void test(Duration a, Duration b) {",
            "    if (a.equals(Duration.ofSeconds(1))) {",
            "      if (a.equals(Duration.ofSeconds(2))) {}",
            "      // BUG: Diagnostic contains:",
            "      if (a.equals(Duration.ofSeconds(1))) {}",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void autoValues() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.auto.value.AutoValue;",
            "class Test {",
            "  public void test(Foo a, Foo b) {",
            "    if (a.bar().equals(\"foo\") && a.bar().equals(b.bar())) {",
            "      // BUG: Diagnostic contains:",
            "      if (a.bar().equals(\"foo\")) {}",
            "      // BUG: Diagnostic contains:",
            "      if (a.bar().equals(b.bar())) {}",
            "    }",
            "  }",
            "  @AutoValue abstract static class Foo {",
            "    abstract String bar();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void autoValue_withEnum() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.auto.value.AutoValue;",
            "class Test {",
            "  public void test(Foo a, Foo b) {",
            "    if (a.bar().equals(E.A)) {",
            "      // BUG: Diagnostic contains:",
            "      if (a.bar().equals(E.A)) {}",
            "    }",
            "  }",
            "  @AutoValue abstract static class Foo {",
            "    abstract E bar();",
            "  }",
            "  private enum E {",
            "    A",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fieldCheckedTwice() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.auto.value.AutoValue;",
            "class Test {",
            "  private final String a = \"foo\";",
            "  public void test(String a) {",
            "    if (this.a.equals(a)) {",
            "      // BUG: Diagnostic contains:",
            "      if (this.a.equals(a)) {}",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
