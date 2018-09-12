/*
 * Copyright 2017 The Error Prone Authors.
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
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link TypeParameterShadowing} */
@RunWith(JUnit4.class)
public class TypeParameterShadowingTest {

  private CompilationTestHelper compilationHelper;
  private BugCheckerRefactoringTestHelper refactoring;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(TypeParameterShadowing.class, getClass());
    refactoring =
        BugCheckerRefactoringTestHelper.newInstance(new TypeParameterShadowing(), getClass());
  }

  @Test
  public void singleLevel() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  // BUG: Diagnostic contains: T declared in Test",
            "  <T> void something() {}",
            "}")
        .doTest();
  }

  @Test
  public void staticNotFlagged() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  static <T> void something() {}",
            "}")
        .doTest();
  }

  @Test
  public void staticMethodInnerDoesntConflictWithOuter() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test<D> {",
            "  static <T> void something() {",
            "    class MethodInner<D> {}", // Doesn't clash with Test<D>
            "    class MethodInner2 {",
            "      // BUG: Diagnostic contains: T declared in something",
            "      <T> void clashingMethod() {}",
            "      <D> void nonClashingMethod() {}", // <D> not in scope
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nestedClassDeclarations() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test<D> {",
            "  // BUG: Diagnostic contains: D declared in Test",
            "  class Test2<D> {}",
            "}")
        .doTest();
  }

  @Test
  public void twoLevels() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  class MyTest<J> {",
            "  // BUG: Diagnostic matches: combo",
            "  public <T,J> void something() {}",
            " }",
            "}")
        .expectErrorMessage(
            "combo", s -> s.contains("J declared in MyTest") && s.contains("T declared in Test"))
        .doTest();
  }

  @Test
  public void renameTypeVar() {
    refactoring
        .addInputLines(
            "in/Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  /** @param <T> foo */",
            "  <T> void something(T t) { T other = t;}",
            "}")
        .addOutputLines(
            "out/Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  /** @param <T2> foo */",
            "  <T2> void something(T2 t) { T2 other = t;}",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void renameRecursiveBound() {
    refactoring
        .addInputLines(
            "in/Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  <T extends Comparable<T>> void something(T t) { T other = t;}",
            "}")
        .addOutputLines(
            "out/Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  <T2 extends Comparable<T2>> void something(T2 t) { T2 other = t;}",
            "}")
        .doTest();
  }

  @Test
  public void refactorUnderneathStuff() {
    refactoring
        .addInputLines(
            "in/Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  <T> void something(T t) { T other = t;}",
            "  <T> T identity(T t) { return t; }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  <T2> void something(T2 t) { T2 other = t;}",
            "  <T2> T2 identity(T2 t) { return t; }",
            "}")
        .doTest();
  }

  @Test
  public void refactorMultipleVars() {
    refactoring
        .addInputLines(
            "in/Test.java",
            "package foo.bar;",
            "class Test<T,D> {",
            "  <T,D> void something(T t) { ",
            "    T other = t;",
            "    java.util.List<T> ts = new java.util.ArrayList<T>();",
            "    D d = null; ",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "package foo.bar;",
            "class Test<T,D> {",
            "  <T2,D2> void something(T2 t) { ",
            "    T2 other = t;",
            "    java.util.List<T2> ts = new java.util.ArrayList<T2>();",
            "    D2 d = null; ",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactorWithNestedTypeParameterDeclaration() {
    // The nested @SuppressWarnings are because there will be multiple findings
    // (T is shadowed multiple times). We're trying to test all of the fixes suggested by the
    // finding generated from the outermost instance method, namely that it doesn't attempt to
    // rewrite symbols T that don't match the T we're rewriting.
    refactoring
        .addInputLines(
            "in/Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  <T,T2> void something(T t) { ",
            "    T var = t;",
            "    @SuppressWarnings(\"TypeParameterShadowing\")",
            "    class MethodInnerWithGeneric<T> {}",
            "    MethodInnerWithGeneric<T> innerVar = null;",
            "    class MethodInner {",
            "       @SuppressWarnings(\"TypeParameterShadowing\")",
            "       <T> void doSomething() {}",
            "       void doSomethingElse(T t) { this.<T>doSomething(); }",
            "    }",
            "    MethodInner myInner = null;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  <T3,T2> void something(T3 t) { ",
            "    T3 var = t;",
            "    @SuppressWarnings(\"TypeParameterShadowing\")",
            "    class MethodInnerWithGeneric<T> {}",
            "    MethodInnerWithGeneric<T3> innerVar = null;",
            "    class MethodInner {",
            "       @SuppressWarnings(\"TypeParameterShadowing\")",
            "       <T> void doSomething() {}",
            "       void doSomethingElse(T3 t) { this.<T3>doSomething(); }",
            "    }",
            "    MethodInner myInner = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactorCheckForExisting() {
    refactoring
        .addInputLines(
            "in/Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  class A<T2,T3,T4> {",
            "    <T> void something(T t) { ",
            "      T var = t;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  class A<T2,T3,T4> {",
            "    <T5> void something(T5 t) { ",
            "      T5 var = t;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactorMethodInnerInner() {
    refactoring
        .addInputLines(
            "in/Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  static <D> void something(D t) {",
            "    class B {",
            "      class C<T,D> {}",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "package foo.bar;",
            "class Test<T> {",
            "  static <D> void something(D t) {",
            "    class B {",
            // T isn't accessible to the inner since the method is static
            "      class C<T,D2> {}",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  // don't try to read type parameters off the enclosing local variable declaration
  @Test
  public void symbolWithoutTypeParameters() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "import java.util.Map;",
            "import java.util.Comparator;",
            "class Test {",
            "  static Comparator<Map.Entry<Integer, String>> ENTRY_COMPARATOR =",
            "    new Comparator<Map.Entry<Integer, String>>() {",
            "      public int compare(",
            "          Map.Entry<Integer, String> o1, Map.Entry<Integer, String> o2) {",
            "        return 0;",
            "      }",
            "      private <T extends Comparable> int c(T o1, T o2) {",
            "        return 0;",
            "      }",
            "    };",
            "}")
        .doTest();
  }

  @Test
  public void lambdaParameterDesugaring() {
    refactoring
        .addInputLines(
            "in/A.java",
            "import java.util.function.Consumer;",
            "class A<T> {",
            "  abstract class B<T> {",
            "    void f() {",
            "      g(t -> {});",
            "    }",
            "    abstract void g(Consumer<T> c);",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java",
            "import java.util.function.Consumer;",
            "class A<T> {",
            "  abstract class B<T2> {",
            "    void f() {",
            "      g(t -> {});",
            "    }",
            "    abstract void g(Consumer<T2> c);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void typesWithBounds() {
    refactoring
        .addInputLines(
            "in/Test.java",
            "import java.util.function.Predicate;",
            "class Test<T> {",
            "  <B extends Object & Comparable> void something(B b) {",
            "    class Foo<B extends Object & Comparable> implements Predicate<B> {",
            "        public boolean test(B b) { return false; }",
            "    }",
            "    new Foo<>();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.function.Predicate;",
            "class Test<T> {",
            "  <B extends Object & Comparable> void something(B b) {",
            "    class Foo<B2 extends Object & Comparable> implements Predicate<B2> {",
            "      public boolean test(B2 b) { return false; }",
            "    }",
            "    new Foo<>();",
            "  }",
            "}")
        .doTest();
  }
}
