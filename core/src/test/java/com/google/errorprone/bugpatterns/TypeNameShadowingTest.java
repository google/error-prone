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
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link TypeNameShadowing} */
@RunWith(JUnit4.class)
public class TypeNameShadowingTest {

  private CompilationTestHelper compilationHelper;
  private BugCheckerRefactoringTestHelper refactoring;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(TypeNameShadowing.class, getClass());
    refactoring = BugCheckerRefactoringTestHelper.newInstance(new TypeNameShadowing(), getClass());
  }

  @Test
  public void positiveClass() {
    compilationHelper
        .addSourceLines(
            "T.java",
            "package foo.bar;", //
            "class T {}")
        .addSourceLines(
            "Foo.java",
            "package foo.bar;", //
            "// BUG: Diagnostic contains: Type parameter T shadows visible type foo.bar.T",
            "class Foo<T> {",
            "  void bar(T t) {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveNestedClass() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "package foo.bar;", //
            "class Foo {",
            "  class T {}",
            "  // BUG: Diagnostic contains: "
                + "Type parameter T shadows visible type foo.bar.Foo$T",
            "  class Bar<T> {",
            "    void bar(T t) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveNestedStaticClass() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "package foo.bar;", //
            "class Foo {",
            "  public static class T {}",
            "  // BUG: Diagnostic contains: "
                + "Type parameter T shadows visible type foo.bar.Foo$T",
            "  class Bar<T> {",
            "    void bar(T t) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveNestedGeneric() {
    compilationHelper
        .addSourceLines(
            "T.java",
            "package foo.bar;", //
            "interface T {}")
        .addSourceLines(
            "Foo.java",
            "package foo.bar;", //
            "class Foo {",
            "  // BUG: Diagnostic contains: Type parameter T shadows visible type foo.bar.T",
            "  class FooInner<T> {",
            "    void bar(T t) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveOtherNestedClass() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "package foo.bar;", //
            "class Foo {",
            "  class T {}",
            "}")
        .addSourceLines(
            "Bar.java",
            "package foo.bar;", //
            "import foo.bar.Foo.T;",
            "// BUG: Diagnostic contains: Type parameter T shadows visible type foo.bar.Foo$T",
            "class Bar<T> {",
            "  void bar(T t) {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveMultipleParamsOneCollides() {
    compilationHelper
        .addSourceLines(
            "T.java",
            "package foo.bar;", //
            "class T {}")
        .addSourceLines(
            "Foo.java",
            "package foo.bar;", //
            "// BUG: Diagnostic contains: Type parameter T shadows visible type foo.bar.T",
            "class Foo<T,U,V> {",
            "  void bar(T t, U u, V v) {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveMultipleParamsBothCollide() {
    compilationHelper
        .addSourceLines(
            "T.java",
            "package foo.bar;", //
            "class T {}")
        .addSourceLines(
            "U.java",
            "package foo.bar;", //
            "class U {}")
        .addSourceLines(
            "Foo.java",
            "package foo.bar;", //
            "// BUG: Diagnostic matches: combo",
            "class Foo<T,U> {",
            "  void bar(T t, U u) {}",
            "}")
        .expectErrorMessage(
            "combo",
            s ->
                s.contains("Type parameter T shadows visible type foo.bar.T")
                    && s.contains("Type parameter U shadows visible type foo.bar.U"))
        .doTest();
  }

  @Test
  public void positiveJavaLangCollision() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "// BUG: Diagnostic contains: Class shadows visible type java.lang.Class", //
            "class Foo<Class> {",
            "  void bar(Class c) {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeClass() {
    compilationHelper
        .addSourceLines(
            "T.java",
            "package foo.bar;", //
            "class T {}")
        .addSourceLines(
            "Foo.java",
            "package foo.bar;", //
            "class Foo<T1> {",
            "  void bar(T1 t) {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeNestedClass() {
    compilationHelper
        .addSourceLines(
            "T.java",
            "package foo.bar;", //
            "class T {",
            "  class Foo<T1> {",
            "    void bar(T1 t) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeOtherNestedClass() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "package foo.bar;", //
            "class Foo {",
            "  class T {}",
            "}")
        .addSourceLines(
            "Bar.java",
            "package foo.bar;", //
            "class Bar<T> {",
            "  void bar(T t) {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeStarImport() {
    compilationHelper
        .addSourceLines(
            "a/T.java",
            "package a;", //
            "class T {}")
        .addSourceLines(
            "b/Foo.java",
            "package b;", //
            "import a.*;",
            "class Foo<T> {",
            "  void bar(T u) {}",
            "}")
        .doTest();
  }

  @Test
  public void refactorSingle() {
    refactoring
        .addInputLines(
            "Foo.java",
            "class Foo {", //
            "  class T {}",
            "  <T> void f(T t){}",
            "}")
        .addOutputLines(
            "Foo.java",
            "class Foo {", //
            "  class T {}",
            "  <T2> void f(T2 t){}",
            "}")
        .doTest();
  }

  @Test
  public void refactorMultiple() {
    refactoring
        .addInputLines(
            "in/Foo.java",
            "class Foo {", //
            "  class T {}",
            "  class U {}",
            "  <T,U> void f(T t, U u){}",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "class Foo {", //
            "  class T {}",
            "  class U {}",
            "  <T2,U2> void f(T2 t, U2 u){}",
            "}")
        .doTest();
  }

  /**
   * Tests that we only suggest a fix when the type variable name is style-guide-adherent;
   * otherwise, it is likely the developer did not mean to use a generic and the fix is misleading
   * Input program here is alpha-equivalent to the refactorMultiple test case; output only fixes T
   */
  @Test
  public void fixOnlyWellNamedVariables() {
    refactoring
        .addInputLines(
            "in/Foo.java",
            "class Foo {", //
            "  class T {}",
            "  class BadParameterName {}",
            "  <T,BadParameterName> void f(T t, BadParameterName u){}",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "class Foo {", //
            "  class T {}",
            "  class BadParameterName {}",
            "  <T2,BadParameterName> void f(T2 t, BadParameterName u){}",
            "}")
        .doTest();
  }
}
