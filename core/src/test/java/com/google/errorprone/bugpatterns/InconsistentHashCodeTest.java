/*
 * Copyright 2018 The Error Prone Authors.
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

/**
 * Tests for {@link InconsistentHashCode} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class InconsistentHashCodeTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(InconsistentHashCode.class, getClass());

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  private int b;",
            "  @Override public boolean equals(Object o) {",
            "    Test that = (Test) o;",
            "    return a == that.a && b == that.b;",
            "  }",
            "  @Override public int hashCode() {",
            "    return a + 31 * b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int foo;",
            "  private int bar;",
            "  @Override public boolean equals(Object o) {",
            "    Test that = (Test) o;",
            "    return foo == that.foo;",
            "  }",
            "  // BUG: Diagnostic contains: bar",
            "  @Override public int hashCode() {",
            "    return foo + 31 * bar;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveViaGetter() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int foo;",
            "  private int bar;",
            "  @Override public boolean equals(Object o) {",
            "    Test that = (Test) o;",
            "    return foo == that.foo;",
            "  }",
            "  // BUG: Diagnostic contains: bar",
            "  @Override public int hashCode() {",
            "    return foo + 31 * getBar();",
            "  }",
            "  private int getBar() { return bar; }",
            "}")
        .doTest();
  }

  @Test
  public void instanceEquality() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  private int b;",
            "  @Override public boolean equals(Object o) {",
            "    return this == o;",
            "  }",
            "  @Override public int hashCode() {",
            "    return a + 31 * b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void memoizedHashCode() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  private int b;",
            "  private int hashCode;",
            "  @Override public boolean equals(Object o) {",
            "    Test that = (Test) o;",
            "    return this.a == that.a && this.b == that.b;",
            "  }",
            "  @Override public int hashCode() {",
            "    return hashCode;",
            "  }",
            "}")
        .doTest();
  }
}
