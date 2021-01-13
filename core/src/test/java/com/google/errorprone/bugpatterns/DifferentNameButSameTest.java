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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link DifferentNameButSame}. */
@RunWith(JUnit4.class)
public final class DifferentNameButSameTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(new DifferentNameButSame(), getClass())
          .addInputLines(
              "TypeUseAnnotation.java",
              "package pkg;",
              "import java.lang.annotation.ElementType;",
              "import java.lang.annotation.Target;",
              "@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})",
              "@interface TypeUseAnnotation {}")
          .expectUnchanged()
          .addInputLines(
              "A.java",
              "package pkg;",
              "public class A {",
              "  public static class B {",
              "    public static class C {",
              "      public static void foo() {}",
              "    }",
              "  }",
              "}")
          .expectUnchanged()
          .addInputLines(
              "ClassAnnotation.java",
              "package pkg;",
              "public @interface ClassAnnotation {",
              "  Class<?> value();",
              "}")
          .expectUnchanged();

  @Test
  public void classReferredToInTwoWays_usesShorterName() {
    helper
        .addInputLines(
            "Test.java",
            "package pkg;",
            "import pkg.A.B;",
            "interface Test {",
            "  A.B test();",
            "  B test2();",
            "}")
        .addOutputLines(
            "Test.java",
            "package pkg;",
            "import pkg.A.B;",
            "interface Test {",
            "  B test();",
            "  B test2();",
            "}")
        .doTest();
  }

  @Test
  public void fullyQualifiedType_notMentioned() {
    helper
        .addInputLines(
            "Test.java",
            "package pkg;",
            "interface Test {",
            "  A.B test();",
            "  pkg.A.B test2();",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void positive2() {
    helper
        .addInputLines(
            "Test.java",
            "package pkg;",
            "import pkg.A.B;",
            "class Test {",
            "  B test() {",
            "    return new A.B();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "package pkg;",
            "import pkg.A.B;",
            "class Test {",
            "  B test() {",
            "    return new B();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void classReferredToInMultipleWaysWithinMemberSelect() {
    helper
        .addInputLines(
            "Test.java",
            "package pkg;",
            "import pkg.A.B;",
            "class Test {",
            "  B.C test() {",
            "    A.B.C.foo();",
            "    return null;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "package pkg;",
            "import pkg.A.B;",
            "class Test {",
            "  B.C test() {",
            "    B.C.foo();",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void typeUseAnnotations_correctlyRefactored() {
    helper
        .addInputLines(
            "Test.java",
            "package pkg;",
            "import pkg.A.B;",
            "interface Test {",
            "  A.@TypeUseAnnotation B test();",
            "  B test2();",
            "}")
        .addOutputLines(
            "Test.java",
            "package pkg;",
            "import pkg.A.B;",
            "interface Test {",
            "  @TypeUseAnnotation B test();",
            "  B test2();",
            "}")
        .doTest();
  }

  @Test
  public void typeUseAnnotation_leftInCorrectPosition() {
    helper
        .addInputLines(
            "Test.java",
            "package pkg;",
            "import pkg.A.B;",
            "interface Test {",
            "  @TypeUseAnnotation B test();",
            "  A.B test2();",
            "}")
        .addOutputLines(
            "Test.java",
            "package pkg;",
            "import pkg.A.B;",
            "interface Test {",
            "  @TypeUseAnnotation B test();",
            "  B test2();",
            "}")
        .doTest();
  }

  @Test
  public void nameShadowed_noChange() {
    helper
        .addInputLines(
            "Test.java",
            "package pkg;",
            "interface Test {",
            "  interface B {",
            "    B get();",
            "  }",
            "  Test.B get();",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void doesNotRefactorOutsideOuterClass() {
    helper
        .addInputLines(
            "Test.java",
            "package pkg;",
            "@ClassAnnotation(A.B.C.class)",
            "class D extends A.B {",
            "  private C c;",
            "  private C c2;",
            "}")
        .addOutputLines(
            "Test.java",
            "package pkg;",
            "@ClassAnnotation(A.B.C.class)",
            "class D extends A.B {",
            "  private A.B.C c;",
            "  private A.B.C c2;",
            "}")
        .doTest();
  }

  @Test
  public void genericsNotRefactored() {
    helper
        .addInputLines(
            "Test.java",
            "package pkg;",
            "interface Test {",
            "  interface A<T> {",
            "    interface D {}",
            "  }",
            "  class B implements A<Long> {}",
            "  class C implements A<String> {}",
            "  B.D b();",
            "  C.D c();",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void typesDefinedWithinSameFileIgnored() {
    helper
        .addInputLines(
            "Test.java",
            "package pkg;",
            "interface Test {",
            "  interface Foo {",
            "    Foo foo();",
            "  }",
            "  Test.Foo foo();",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void typesWhichMakePoorImports_disfavoured() {
    helper
        .addInputLines("Foo.java", "package pkg;", "interface Foo {", "  interface Builder {}", "}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "package pkg;",
            "import pkg.Foo.Builder;",
            "interface Test {",
            "  Foo.Builder a();",
            "  Builder b();",
            "}")
        .addOutputLines(
            "Test.java",
            "package pkg;",
            "import pkg.Foo.Builder;",
            "interface Test {",
            "  Foo.Builder a();",
            "  Foo.Builder b();",
            "}")
        .doTest();
  }

  @Test
  public void classClashesWithVariableName() {
    helper
        .addInputLines(
            "Test.java",
            "package pkg;",
            "import pkg.A.B;",
            "class Test {",
            "  A.B test(Object B) {",
            "    return new A.B();",
            "  }",
            "  B test2() {",
            "    return null;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "package pkg;",
            "import pkg.A.B;",
            "class Test {",
            "  A.B test(Object B) {",
            "    return new A.B();",
            "  }",
            "  A.B test2() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Ignore("b/177381438")
  @Test
  public void innerClassConstructor() {
    BugCheckerRefactoringTestHelper.newInstance(new DifferentNameButSame(), getClass())
        .addInputLines(
            "A.java", //
            "package pkg;",
            "class A {",
            "  class B {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "package pkg;",
            "class Test {",
            "  static void f(A a) {",
            "    A.B b = a.new B();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
