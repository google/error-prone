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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ClassCanBeStatic}Test */
@RunWith(JUnit4.class)
public class ClassCanBeStaticTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ClassCanBeStatic.class, getClass());
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("ClassCanBeStaticNegativeCases.java").doTest();
  }

  @Test
  public void testPositiveCase1() {
    compilationHelper.addSourceFile("ClassCanBeStaticPositiveCase1.java").doTest();
  }

  @Test
  public void testPositiveCase2() {
    compilationHelper.addSourceFile("ClassCanBeStaticPositiveCase2.java").doTest();
  }

  @Test
  public void testPositiveCase3() {
    compilationHelper.addSourceFile("ClassCanBeStaticPositiveCase3.java").doTest();
  }

  @Test
  public void positiveReference() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  private class One { int field;  }",
            "  // BUG: Diagnostic contains:",
            "  private class Two { String field; }",
            "}")
        .doTest();
  }

  @Test
  public void nonMemberField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int x;",
            "  private class One {",
            "    {",
            "      System.err.println(x);",
            "    }",
            "  }",
            "  // BUG: Diagnostic contains:",
            "  private class Two {",
            "    void f(Test t) {",
            "      System.err.println(t.x);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifiedThis() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private class One {",
            "    {",
            "      System.err.println(Test.this);",
            "    }",
            "  }",
            "  // BUG: Diagnostic contains:",
            "  private class Two {",
            "    void f(Test t) {",
            "      System.err.println(Test.class);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void referencesSibling() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private class One {",
            "    {",
            "      new Two();",
            "    }",
            "  }",
            "  private class Two {",
            "    void f(Test t) {",
            "      System.err.println(Test.this);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void referenceInAnonymousClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private class Two {",
            "    {",
            "      new Runnable() {",
            "        @Override public void run() {",
            "          System.err.println(Test.this);",
            "        }",
            "      }.run();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void extendsInnerClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private class One {",
            "    {",
            "      System.err.println(Test.this);",
            "    }",
            "  }",
            "  private class Two extends One {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ctorParametricInnerClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private class One<T> {",
            "    {",
            "      System.err.println(Test.this);",
            "    }",
            "  }",
            "  private abstract class Two {",
            "    {",
            "      new One<String>();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void extendsParametricInnerClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private class One<T> {",
            "    {",
            "      System.err.println(Test.this);",
            "    }",
            "  }",
            "  private abstract class Two<T> extends One<T> {}",
            "}")
        .doTest();
  }

  @Test
  public void referencesTypeParameter() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test<T> {",
            "  private class One {",
            "    List<T> xs;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void referencesTypeParameterImplicit() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test<T> {",
            "  class One {",
            "    {",
            "      System.err.println(Test.this);",
            "    }",
            "  }",
            "  class Two {",
            "    One one; // implicit reference of Test<T>.One",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_referencesTypeParameterImplicit() {
    compilationHelper
        .addSourceLines(
            "One.java",
            "package test;",
            "public class One<T> {",
            "  public class Inner {",
            "    {",
            "      System.err.println(One.this);",
            "    }",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "import test.One.Inner;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "   class Two {",
            "    Inner inner; // ok: implicit reference of One.Inner",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifiedSuperReference() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  class One {",
            "    {",
            "      Test.super.getClass();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void annotationMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  class One {",
            "    @SuppressWarnings(value = \"\")",
            "    void f() {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void extendsHiddenInnerClass() {
    compilationHelper
        .addSourceLines(
            "A.java", //
            "public class A {",
            "  public class Inner {",
            "    {",
            "      System.err.println(A.this);",
            "    }",
            "  }",
            "}")
        .addSourceLines(
            "B.java", //
            "public class B extends A {",
            "  public class Inner extends A.Inner {}",
            "}")
        .doTest();
  }

  @Test
  public void nestedInAnonymous() {
    compilationHelper
        .addSourceLines(
            "A.java", //
            "public class A {",
            "  static Runnable r =",
            "    new Runnable() {",
            "      class Inner {",
            "      }",
            "      public void run() {}",
            "    };",
            "}")
        .doTest();
  }

  @Test
  public void nestedInLocal() {
    compilationHelper
        .addSourceLines(
            "A.java", //
            "public class A {",
            "  static void f() {",
            "    class Outer {",
            "      class Inner {",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void innerClassMethodReference() {
    compilationHelper
        .addSourceLines(
            "T.java", //
            "import java.util.function.Supplier;",
            "public class T {",
            "  class A {",
            "    {",
            "      System.err.println(T.this);",
            "    }",
            "  }",
            "  class B {",
            "    {",
            "      Supplier<A> s = A::new; // capture enclosing instance",
            "      System.err.println(s.get());",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
