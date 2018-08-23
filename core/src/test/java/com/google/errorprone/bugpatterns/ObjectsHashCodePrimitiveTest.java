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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ObjectsHashCodePrimitive}. */
@RunWith(JUnit4.class)
public class ObjectsHashCodePrimitiveTest {

  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(new ObjectsHashCodePrimitive(), getClass());

  @Test
  public void hashCodeIntLiteral() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    int y = Objects.hashCode(3);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    int y = Integer.hashCode(3);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hashCodeByte() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    byte x = 3;",
            "    int y = Objects.hashCode(x);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    byte x = 3;",
            "    int y = Byte.hashCode(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hashCodeShort() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    short x = 3;",
            "    int y = Objects.hashCode(x);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    short x = 3;",
            "    int y = Short.hashCode(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hashCodeInt() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    int x = 3;",
            "    int y = Objects.hashCode(x);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    int x = 3;",
            "    int y = Integer.hashCode(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hashCodeLong() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    long x = 3;",
            "    int y = Objects.hashCode(x);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    long x = 3;",
            "    int y = Long.hashCode(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hashCodeFloat() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    float x = 3;",
            "    int y = Objects.hashCode(x);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    float x = 3;",
            "    int y = Float.hashCode(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hashCodeDouble() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    double x = 3;",
            "    int y = Objects.hashCode(x);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    double x = 3;",
            "    int y = Double.hashCode(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hashCodeChar() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    char x = 'C';",
            "    int y = Objects.hashCode(x);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    char x = 'C';",
            "    int y = Character.hashCode(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hashCodeBoolean() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    boolean x = true;",
            "    int y = Objects.hashCode(x);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  void f() {",
            "    boolean x = true;",
            "    int y = Boolean.hashCode(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hashCodeClassVariable() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  boolean x = true;",
            "  void f() {",
            "    int y = Objects.hashCode(x);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  boolean x = true;",
            "  void f() {",
            "    int y = Boolean.hashCode(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hashCodeObjectNegative() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  Object o = new Object();",
            "  void f() {",
            "    int y = Objects.hashCode(o);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void hashCodeBoxedPrimitiveNegative() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  Integer x = new Integer(3);",
            "  void f() {",
            "    int y = Objects.hashCode(x);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void hashCodeOtherMethodNegative() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Objects;",
            "class Test {",
            "  Integer x = new Integer(3);",
            "  void f() {",
            "    int y = x.hashCode();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
