/*
 * Copyright 2012 The Error Prone Authors.
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

/** Tests for {@link NonOverridingEquals}. */
// TODO(eaftan): Tests for correctness of suggested fix
@RunWith(JUnit4.class)
public class NonOverridingEqualsTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(NonOverridingEquals.class, getClass());

  // Positive cases

  @Test
  public void testFlagsSimpleCovariantEqualsMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains: Did you mean '@Override'",
            "  public boolean equals(Test other) {",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }

  // The following two tests are really to help debug the construction of the suggested fixes.

  @Test
  public void testFlagsComplicatedCovariantEqualsMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  int i, j, k;",
            "  // BUG: Diagnostic contains: Did you mean '@Override'",
            "  public boolean equals(Test other) {",
            "    if (i == other.i && j == other.j && k == other.k) {",
            "      return true;",
            "    }",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFlagsAnotherComplicatedCovariantEqualsMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  boolean isInVersion;",
            "  String str;",
            "  // BUG: Diagnostic contains: Did you mean '@Override'",
            "  public boolean equals(Test that) {",
            "    return (this.isInVersion == that.isInVersion)",
            "        && this.str.equals(that.str);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFlagsAbstractCovariantEqualsMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public abstract class Test {",
            "  // BUG: Diagnostic contains: Did you mean '@Override'",
            "  public abstract boolean equals(Test other);",
            "}")
        .doTest();
  }

  @Test
  public void testFlagsNativeCovariantEqualsMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains: Did you mean '@Override'",
            "  public native boolean equals(Test other);",
            "}")
        .doTest();
  }

  @Test
  public void testFlagsIfMethodTakesUnrelatedType() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  public boolean equals(Integer other) {",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFlagsBoxedBooleanReturnType() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  public Boolean equals(Test other) {",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFlagsCovariantEqualsMethodInEnum() {
    compilationHelper
        .addSourceLines(
            "Planet.java",
            "public enum Planet {",
            "  MERCURY,",
            "  VENUS,",
            "  EARTH,",
            "  MARS,",
            "  JUPITER,",
            "  SATURN,",
            "  URANUS,",
            "  NEPTUNE;", // Pluto: never forget
            "  // BUG: Diagnostic contains: enum instances can safely be compared by reference "
                + "equality",
            "  // Did you mean to remove this line?",
            "  public boolean equals(Planet other) {",
            "    return this == other;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFlagsPrivateEqualsMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  private boolean equals(Test other) {",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFlagsEvenIfAnotherMethodOverridesEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains: either inline it into the callers or rename it",
            "  private boolean equals(Test other) {",
            "    return false;",
            "  }",
            "  @Override public boolean equals(Object other) {",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }

  /**
   * A static method can be invoked on an instance, so a static equals method with one argument
   * could be confused with Object#equals. Though I can't imagine how anyone would define a
   * single-argument static equals method...
   */
  @Test
  public void testFlagsStaticEqualsMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  public static boolean equals(Test other) {",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }

  // Negative cases

  @Test
  public void testDontFlagMethodThatOverridesEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  @Override public boolean equals(Object other) {",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDontFlagEqualsMethodWithMoreThanOneParameter() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public boolean equals(Test other, String s) {",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDontFlagIfWrongReturnType() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public int equals(Test other) {",
            "    return -1;",
            "  }",
            "}")
        .doTest();
  }
}
