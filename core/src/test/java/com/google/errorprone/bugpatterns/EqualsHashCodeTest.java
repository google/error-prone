/*
 * Copyright 2014 The Error Prone Authors.
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

/** {@link EqualsHashCode}Test */
@RunWith(JUnit4.class)
public class EqualsHashCodeTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(EqualsHashCode.class, getClass());

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("EqualsHashCodeTestPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("EqualsHashCodeTestNegativeCases.java").doTest();
  }

  @Test
  public void superClassWithoutHashCode() {
    compilationHelper
        .addSourceLines("Super.java", "abstract class Super {}")
        .addSourceLines(
            "Test.java",
            "class Test extends Super {",
            "  // BUG: Diagnostic contains:",
            "  public boolean equals(Object o) { return false; }",
            "}")
        .doTest();
  }

  @Test
  public void inherited() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "class Super {",
            "  public int hashCode() {",
            "    return 42;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test extends Super {",
            "  public boolean equals(Object o) { return false; }",
            "}")
        .doTest();
  }

  @Test
  public void interfaceEquals() {
    compilationHelper
        .addSourceLines("I.java", "interface I {", "  boolean equals(Object o);", "}")
        .doTest();
  }

  @Test
  public void abstractHashCode() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "abstract class Super {",
            "  public abstract boolean equals(Object o);",
            "  public abstract int hashCode();",
            "}")
        .doTest();
  }

  @Test
  public void abstractNoHashCode() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "abstract class Super {",
            "  // BUG: Diagnostic contains:",
            "  public abstract boolean equals(Object o);",
            "}")
        .doTest();
  }

  @Test
  public void suppressOnEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @SuppressWarnings(\"EqualsHashCode\")",
            "  public boolean equals(Object o) { return false; }",
            "}")
        .doTest();
  }

  @Test
  public void nopEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public boolean equals(Object o) {",
            "    return super.equals(o);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nopEqualsWithNullable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.Nullable;",
            "class Test {",
            "  public boolean equals(@Nullable Object o) {",
            "    return super.equals(o);",
            "  }",
            "}")
        .doTest();
  }
}
