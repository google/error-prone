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
 * Tests for {@link EqualsWrongThing} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class EqualsWrongThingTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(EqualsWrongThing.class, getClass());

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Objects;",
            "class Test {",
            "  private int a;",
            "  private int b;",
            "  @Override public boolean equals(Object o) {",
            "    Test that = (Test) o;",
            "    return a == that.a && Objects.equal(b, that.b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeUnordered() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  private int b;",
            "  @Override public boolean equals(Object o) {",
            "    Test that = (Test) o;",
            "    return (a == that.a && b == that.b) || (a == that.b && b == that.a);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeMixOfGettersAndFields() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  private int b;",
            "  private Object c;",
            "  private int getA() { return a; }",
            "  private int getB() { return b; }",
            "  @Override public boolean equals(Object o) {",
            "    Test that = (Test) o;",
            "    return this.a == that.getA() && this.b == that.getB() && c.equals(that.c);",
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
            "  private int a;",
            "  private int b;",
            "  @Override public boolean equals(Object o) {",
            "    Test that = (Test) o;",
            "    // BUG: Diagnostic contains: comparison between `a` and `b`",
            "    return a == that.b && b == that.b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveGetters() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  private int b;",
            "  private int getA() { return a; }",
            "  private int getB() { return b; }",
            "  @Override public boolean equals(Object o) {",
            "    Test that = (Test) o;",
            "    // BUG: Diagnostic contains: comparison between `getA()` and `getB()`",
            "    return getA() == that.getB() && getB() == that.getB();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveObjects() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Objects;",
            "class Test {",
            "  private int a;",
            "  private int b;",
            "  @Override public boolean equals(Object o) {",
            "    Test that = (Test) o;",
            "    // BUG: Diagnostic contains: comparison between `a` and `b`",
            "    return Objects.equal(a, that.b) && b == that.b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveEquals() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private Object a;",
            "  private Object b;",
            "  @Override public boolean equals(Object o) {",
            "    Test that = (Test) o;",
            "    // BUG: Diagnostic contains: comparison between `a` and `b`",
            "    return a.equals(that.b) && b == that.b;",
            "  }",
            "}")
        .doTest();
  }
}
