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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TypeParameterUnusedInFormalsTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(TypeParameterUnusedInFormals.class, getClass());
  }

  @Test
  public void evilCastImpl() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  static <T> T doCast(Object o) { T t = (T) o; return t; }",
            "}")
        .doTest();
  }

  @Test
  public void leadingParam() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  static <U extends Object, T> T doCast(U o) { T t = (T) o; return t; }",
            "}")
        .doTest();
  }

  @Test
  public void trailingParam() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  static <T, U extends Object> T doCast(U o) { T t = (T) o; return t; }",
            "}")
        .doTest();
  }

  @Test
  public void leadingAndTrailingParam() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  static <V extends Object, T, U extends Object> T doCast(U o, V v) { T t = (T) o; return t; }",
            "}")
        .doTest();
  }

  @Test
  public void superBound() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  static <T extends Number> T doCast(Object o) { return (T) o; }",
            "}")
        .doTest();
  }

  @Test
  public void okFBound() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test {",
            "  interface Foo<T> {}",
            "  static <T extends Foo<T>> T doCast(Object o) { return (T) o; }",
            "}")
        .doTest();
  }

  @Test
  public void wildbound() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test {",
            "  interface Foo<T> {}",
            "  // BUG: Diagnostic contains:",
            "  static <T extends Foo<?>> T doCast(Object o) { return (T) o; }",
            "}")
        .doTest();
  }

  @Test
  public void okGenericFactory() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test {",
            "  static <T> List<T> newList() { return null; }",
            "}")
        .doTest();
  }

  @Test
  public void okWithParam() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test {",
            "  static <T> T noop(T t) { return t; }",
            "}")
        .doTest();
  }

  @Test
  public void okNotMyParam() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test<T> {",
            "  T noop(T t) { return t; }",
            "}")
        .doTest();
  }

  @Test
  public void abstractMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  // BUG: Diagnostic contains:",
            "  abstract <T> T badMethod();",
            "}")
        .doTest();
  }

  @Test
  public void objectCast() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  <T> T badMethod(String s) { return (T) s; }",
            "}")
        .doTest();
  }

  @Test
  public void issue343() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  interface Visitor1<X, Y> {}",
            "  interface Visitor2<X, Y> {}",
            "  <R,",
            "    R1 extends R,",
            "    R2 extends R,",
            "    X1 extends Exception,",
            "    X2 extends Exception,",
            "    V extends Visitor1<R1, X1> & Visitor2<R2, X2>>",
            "  R accept_(V v) throws X1, X2;",
            "}")
        .doTest();
  }

  // regression test for b/28055418
  @Test
  public void classTypeParameter() {
    compilationHelper
        .addSourceLines("Test.java", "abstract class Test<T> {", "  abstract T get(String s);", "}")
        .doTest();
  }

  // regression test for b/35385704
  @Test
  public void typeAnnotation() {
    compilationHelper
        .addSourceLines(
            "A.java",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Target;",
            "@Target(ElementType.TYPE_USE)",
            "@interface A {}")
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  <T> T f(@A T x) {",
            "    return x;",
            "  }",
            "}")
        .doTest();
  }
}
