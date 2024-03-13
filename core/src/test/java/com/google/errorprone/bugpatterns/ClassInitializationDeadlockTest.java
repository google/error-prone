/*
 * Copyright 2024 The Error Prone Authors.
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

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ClassInitializationDeadlockTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(ClassInitializationDeadlock.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "A.java",
            "public class A {",
            "  // BUG: Diagnostic contains:",
            "  private static Object cycle = new B();",
            "  public static class B extends A {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeMethod() {
    testHelper
        .addSourceLines(
            "A.java",
            "public class A {",
            "  private static Object cycle = new Object() {",
            "    void f() {",
            "      new B();",
            "    }",
            "  };",
            "  public static class B extends A {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeEnum() {
    testHelper
        .addSourceLines(
            "E.java", //
            "enum E {",
            "  ONE(0),",
            "  TWO {",
            "    void f() {}",
            "  };",
            "  E(int x) {}",
            "  E() {}",
            "}")
        .doTest();
  }

  @Test
  public void negativePrivate() {
    testHelper
        .addSourceLines(
            "A.java",
            "public class A {",
            "  private static Object benign_cycle = new B.C();",
            "  private static class B {",
            "    public static class C extends A { }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeAutoValue() {
    testHelper
        .setArgs("-processor", AutoValueProcessor.class.getName())
        .addSourceLines(
            "A.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Animal {",
            "  private static final Animal WALLABY = new AutoValue_Animal(\"Wallaby\", 4);",
            "  static Animal create(String name, int numberOfLegs) {",
            "    return new AutoValue_Animal(name, numberOfLegs);",
            "  }",
            "  abstract String name();",
            "  abstract int numberOfLegs();",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "A.java",
            "public class A {",
            "  static Object constant = 1;",
            "  Object nonStatic = new B();",
            "  static Class<?> classLiteral = B.class;",
            "  static class B extends A {}",
            "}")
        .doTest();
  }
}
