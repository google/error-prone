/*
 * Copyright 2019 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link SameNameButDifferent}. */
@RunWith(JUnit4.class)
public final class SameNameButDifferentTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(SameNameButDifferent.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(SameNameButDifferent.class, getClass());

  @Test
  public void simpleNameClash() {
    helper
        .addSourceLines(
            "A.java", //
            "class A {",
            "  class Supplier {}",
            "}")
        .addSourceLines(
            "B.java",
            "import java.util.function.Supplier;",
            "class B {",
            "  // BUG: Diagnostic contains:",
            "  Supplier<Integer> supplier = () -> 1;",
            "  class C extends A {",
            "    // BUG: Diagnostic contains:",
            "    Supplier supplier2 = new Supplier();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void simpleNameRefactoring() {
    refactoring
        .addInputLines(
            "A.java", //
            "package foo;",
            "class A {",
            "  class Supplier {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "B.java",
            "package foo;",
            "import java.util.function.Supplier;",
            "class B {",
            "  Supplier<Integer> supplier = () -> 1;",
            "  class C extends A {",
            "    Supplier supplier2 = new Supplier();",
            "    Class<Supplier> clazz = Supplier.class;",
            "  }",
            "}")
        .addOutputLines(
            "B.java",
            "package foo;",
            "import java.util.function.Supplier;",
            "class B {",
            "  Supplier<Integer> supplier = () -> 1;",
            "  class C extends A {",
            "    A.Supplier supplier2 = new A.Supplier();",
            "    Class<A.Supplier> clazz = A.Supplier.class;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nestedClassNameClash() {
    refactoring
        .addInputLines(
            "A.java", //
            "package foo;",
            "class A {",
            "  static class B {",
            "    static class C {}",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "D.java",
            "package foo;",
            "class D {",
            "  static class B {",
            "    static class C {}",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "E.java",
            "package foo;",
            "import foo.A.B;",
            "class E {",
            "  B.C foo = new B.C();",
            "  class C extends D {",
            "    B.C bar = new B.C();",
            "  }",
            "}")
        .addOutputLines(
            "E.java",
            "package foo;",
            "import foo.A.B;",
            "class E {",
            "  A.B.C foo = new A.B.C();",
            "  class C extends D {",
            "    D.B.C bar = new D.B.C();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeAlreadyQualified() {
    helper
        .addSourceLines(
            "A.java", //
            "class A {",
            "  class Supplier {}",
            "}")
        .addSourceLines(
            "B.java",
            "import java.util.function.Supplier;",
            "class B {",
            "  Supplier<Integer> supplier = () -> 1;",
            "  class C extends A {",
            "    A.Supplier supplier2 = new A.Supplier();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void neverShadowing() {
    helper
        .addSourceLines(
            "A.java",
            "class A {",
            "  public void foo() {",
            "    class B {}",
            "    B b = new B();",
            "  }",
            "  public void bar() {",
            "    class B {}",
            "    B b = new B();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void doesNotThrowConcurrentModification_whenFilteringNonShadowingTypeNames() {
    helper
        .addSourceLines(
            "A.java",
            "class A {",
            "  public void foo() {",
            "    class B {}",
            "    B b = new B();",
            "  }",
            "  public void bar() {",
            "    class B {}",
            "    class C {}",
            "    C c = new C();",
            "    B b = new B();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void referencesSelf() {
    helper
        .addSourceLines(
            "B.java",
            "import java.util.function.Supplier;",
            "class B {",
            "  Supplier<Integer> supplier = () -> 1;",
            "  class C {",
            "  class Supplier {",
            "    Supplier s = new Supplier();",
            "  }",
            "  }",
            "}")
        .doTest();
  }
}
