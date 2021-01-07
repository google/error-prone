/*
 * Copyright 2020 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author vlk@google.com (Volodymyr Kachurovskyi) */
@RunWith(JUnit4.class)
public class ObjectEqualsForPrimitivesTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(ObjectEqualsForPrimitives.class, getClass());

  @Test
  public void testBoxedIntegers() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Objects;",
            "public class Test {",
            "  private static boolean doTest(Integer a, Integer b) {",
            "    return Objects.equals(a, b);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testBoxedAndPrimitive() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Objects;",
            "public class Test {",
            "  private static boolean doTest(Integer a, int b) {",
            "    return Objects.equals(a, b);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testPrimitiveAndBoxed() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Objects;",
            "public class Test {",
            "  private static boolean doTest(int a, Integer b) {",
            "    return Objects.equals(a, b);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testObjects() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Objects;",
            "public class Test {",
            "  private static boolean doTest(Object a, Object b) {",
            "    return Objects.equals(a, b);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testPrimitives() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Objects;",
            "public class Test {",
            "  private static boolean testBooleans(boolean a, boolean b) {",
            "    return Objects.equals(a, b);",
            "  }",
            "  private static boolean testInts(int a, int b) {",
            "    return Objects.equals(a, b);",
            "  }",
            "  private static boolean testLongs(long a, long b) {",
            "    return Objects.equals(a, b);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Objects;",
            "public class Test {",
            "  private static boolean testBooleans(boolean a, boolean b) {",
            "    return (a == b);",
            "  }",
            "  private static boolean testInts(int a, int b) {",
            "    return (a == b);",
            "  }",
            "  private static boolean testLongs(long a, long b) {",
            "    return (a == b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPrimitivesNegated() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Objects;",
            "public class Test {",
            "  private static boolean testBooleans(boolean a, boolean b) {",
            "    return !Objects.equals(a, b);",
            "  }",
            "  private static boolean testInts(int a, int b) {",
            "    return !Objects.equals(a, b);",
            "  }",
            "  private static boolean testLongs(long a, long b) {",
            "    return !Objects.equals(a, b);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Objects;",
            "public class Test {",
            "  private static boolean testBooleans(boolean a, boolean b) {",
            "    return !(a == b);",
            "  }",
            "  private static boolean testInts(int a, int b) {",
            "    return !(a == b);",
            "  }",
            "  private static boolean testLongs(long a, long b) {",
            "    return !(a == b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testIntAndLong() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Objects;",
            "public class Test {",
            "  private static boolean doTest(int a, long b) {",
            "    return Objects.equals(a, b);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Objects;",
            "public class Test {",
            "  private static boolean doTest(int a, long b) {",
            "    return (a == b);",
            "  }",
            "}")
        .doTest();
  }
}
