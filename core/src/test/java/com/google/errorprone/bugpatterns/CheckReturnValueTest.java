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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.auto.value.processor.AutoBuilderProcessor;
import com.google.auto.value.processor.AutoValueProcessor;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class CheckReturnValueTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(CheckReturnValue.class, getClass());

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testPositiveCases() {
    compilationHelper.addSourceFile("CheckReturnValuePositiveCases.java").doTest();
  }

  @Test
  public void testCustomCheckReturnValueAnnotation() {
    compilationHelper
        .addSourceLines(
            "foo/bar/CheckReturnValue.java",
            "package foo.bar;",
            "public @interface CheckReturnValue {}")
        .addSourceLines(
            "test/TestCustomCheckReturnValueAnnotation.java",
            "package test;",
            "import foo.bar.CheckReturnValue;",
            "public class TestCustomCheckReturnValueAnnotation {",
            "  @CheckReturnValue",
            "  public String getString() {",
            "    return \"string\";",
            "  }",
            "  public void doIt() {",
            "    // BUG: Diagnostic contains:",
            "    getString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testCustomCanIgnoreReturnValueAnnotation() {
    compilationHelper
        .addSourceLines(
            "foo/bar/CanIgnoreReturnValue.java",
            "package foo.bar;",
            "public @interface CanIgnoreReturnValue {}")
        .addSourceLines(
            "test/TestCustomCanIgnoreReturnValueAnnotation.java",
            "package test;",
            "import foo.bar.CanIgnoreReturnValue;",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class TestCustomCanIgnoreReturnValueAnnotation {",
            "  @CanIgnoreReturnValue",
            "  public String ignored() {",
            "    return null;",
            "  }",
            "  public void doIt() {",
            "    ignored();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("CheckReturnValueNegativeCases.java").doTest();
  }

  @Test
  public void testPackageAnnotation() {
    compilationHelper
        .addSourceLines(
            "package-info.java", //
            "@com.google.errorprone.annotations.CheckReturnValue",
            "package lib;")
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "public class Lib {",
            "  public static int f() { return 42; }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    // BUG: Diagnostic contains: ",
            "    lib.Lib.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testClassAnnotation() {
    compilationHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Lib {",
            "  public static int f() { return 42; }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    // BUG: Diagnostic contains: ",
            "    lib.Lib.f();",
            "  }",
            "}")
        .doTest();
  }

  // Don't match void-returning methods in packages with @CRV
  @Test
  public void testVoidReturningMethodInAnnotatedPackage() {
    compilationHelper
        .addSourceLines(
            "package-info.java", //
            "@com.google.errorprone.annotations.CheckReturnValue",
            "package lib;")
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "public class Lib {",
            "  public static void f() {}",
            "}")
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void m() {",
            "    lib.Lib.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void badCRVOnProcedure() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package lib;",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  // @CheckReturnValue may not be applied to void-returning methods",
            "  @com.google.errorprone.annotations.CheckReturnValue public static void f() {}",
            "}")
        .doTest();
  }

  @Test
  public void badCRVOnPseudoProcedure() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package lib;",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  // @CheckReturnValue may not be applied to void-returning methods",
            "  @com.google.errorprone.annotations.CheckReturnValue public static Void f() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  // Don't match methods invoked through {@link org.mockito.Mockito}.
  @Test
  public void testIgnoreCRVOnMockito() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package lib;",
            "public class Test {",
            "  @com.google.errorprone.annotations.CheckReturnValue",
            " public int f() {",
            "    return 0;",
            "  }",
            "}")
        .addSourceLines(
            "TestCase.java",
            "import static org.mockito.Mockito.verify;",
            "import static org.mockito.Mockito.doReturn;",
            "import org.mockito.Mockito;",
            "class TestCase {",
            "  void m() {",
            "    lib.Test t = new lib.Test();",
            "    Mockito.verify(t).f();",
            "    verify(t).f();",
            "    doReturn(1).when(t).f();",
            "    Mockito.doReturn(1).when(t).f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPackageAnnotationButCanIgnoreReturnValue() {
    compilationHelper
        .addSourceLines(
            "package-info.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "package lib;")
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "public class Lib {",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  public static int f() { return 42; }",
            "}")
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void m() {",
            "    lib.Lib.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testClassAnnotationButCanIgnoreReturnValue() {
    compilationHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Lib {",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  public static int f() { return 42; }",
            "}")
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void m() {",
            "    lib.Lib.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void badCanIgnoreReturnValueOnProcedure() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package lib;",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  // @CanIgnoreReturnValue may not be applied to void-returning methods",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue public static void f() {}",
            "}")
        .doTest();
  }

  @Test
  public void testNestedClassAnnotation() {
    compilationHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Lib {",
            "  public static class Inner {",
            "    public static class InnerMost {",
            "      public static int f() { return 42; }",
            "    }",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    // BUG: Diagnostic contains: ",
            "    lib.Lib.Inner.InnerMost.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNestedClassWithCanIgnoreAnnotation() {
    compilationHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Lib {",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  public static class Inner {",
            "    public static class InnerMost {",
            "      public static int f() { return 42; }",
            "    }",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    lib.Lib.Inner.InnerMost.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPackageWithCanIgnoreAnnotation() {
    compilationHelper
        .addSourceLines(
            "package-info.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "package lib;")
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "@com.google.errorprone.annotations.CanIgnoreReturnValue",
            "public class Lib {",
            "  public static int f() { return 42; }",
            "}")
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void m() {",
            "    lib.Lib.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void errorBothClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "@com.google.errorprone.annotations.CanIgnoreReturnValue",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "// BUG: Diagnostic contains: @CheckReturnValue and @CanIgnoreReturnValue cannot"
                + " both be applied to the same class",
            "class Test {}")
        .doTest();
  }

  @Test
  public void errorBothMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  @com.google.errorprone.annotations.CheckReturnValue",
            "  // BUG: Diagnostic contains: @CheckReturnValue and @CanIgnoreReturnValue cannot"
                + " both be applied to the same method",
            "  void m() {}",
            "}")
        .doTest();
  }

  // Don't match Void-returning methods in packages with @CRV
  @Test
  public void testJavaLangVoidReturningMethodInAnnotatedPackage() {
    compilationHelper
        .addSourceLines(
            "package-info.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "package lib;")
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "public class Lib {",
            "  public static Void f() {",
            "    return null;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void m() {",
            "    lib.Lib.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInTests() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Foo {",
            "  public int f() {",
            "    return 42;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(Foo foo) {",
            "    try {",
            "      foo.f();",
            "      org.junit.Assert.fail();",
            "    } catch (Exception expected) {}",
            "    try {",
            "      foo.f();",
            "      junit.framework.Assert.fail();",
            "    } catch (Exception expected) {}",
            "    try {",
            "      foo.f();",
            "      junit.framework.TestCase.fail();",
            "    } catch (Exception expected) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInTestsWithRule() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Foo {",
            "  public int f() {",
            "    return 42;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private org.junit.rules.ExpectedException exception;",
            "  void f(Foo foo) {",
            "    exception.expect(IllegalArgumentException.class);",
            "    foo.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInTestsWithFailureMessage() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Foo {",
            "  public int f() {",
            "    return 42;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(Foo foo) {",
            "    try {",
            "      foo.f();",
            "      org.junit.Assert.fail(\"message\");",
            "    } catch (Exception expected) {}",
            "    try {",
            "      foo.f();",
            "      junit.framework.Assert.fail(\"message\");",
            "    } catch (Exception expected) {}",
            "    try {",
            "      foo.f();",
            "      junit.framework.TestCase.fail(\"message\");",
            "    } catch (Exception expected) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInThrowingRunnables() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Foo {",
            "  public int f() {",
            "    return 42;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(Foo foo) {",
            "   org.junit.Assert.assertThrows(IllegalStateException.class, ",
            "     new org.junit.function.ThrowingRunnable() {",
            "       @Override",
            "       public void run() throws Throwable {",
            "         foo.f();",
            "       }",
            "     });",
            "   org.junit.Assert.assertThrows(IllegalStateException.class, () -> foo.f());",
            "   org.junit.Assert.assertThrows(IllegalStateException.class, foo::f);",
            "   org.junit.Assert.assertThrows(IllegalStateException.class, () -> {",
            "      int bah = foo.f();",
            "      foo.f(); ",
            "   });",
            "   org.junit.Assert.assertThrows(IllegalStateException.class, () -> { ",
            "     // BUG: Diagnostic contains: ",
            "     foo.f(); ",
            "     foo.f(); ",
            "   });",
            "   bar(() -> foo.f());",
            "   org.assertj.core.api.Assertions.assertThatExceptionOfType(IllegalStateException.class)",
            "      .isThrownBy(() -> foo.f());",
            "  }",
            "  void bar(org.junit.function.ThrowingRunnable r) {}",
            "}")
        .doTest();
  }

  @Test
  public void ignoreTruthFailure() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Foo {",
            "  public int f() {",
            "    return 42;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assert_;",
            "class Test {",
            "  void f(Foo foo) {",
            "    try {",
            "      foo.f();",
            "      assert_().fail();",
            "    } catch (Exception expected) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void onlyIgnoreWithEnclosingTryCatch() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Foo {",
            "  public int f() {",
            "    return 42;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "import static org.junit.Assert.fail;",
            "class Test {",
            "  void f(Foo foo) {",
            "    // BUG: Diagnostic contains: ",
            "    foo.f();",
            "    org.junit.Assert.fail();",
            "    // BUG: Diagnostic contains: ",
            "    foo.f();",
            "    junit.framework.Assert.fail();",
            "    // BUG: Diagnostic contains: ",
            "    foo.f();",
            "    junit.framework.TestCase.fail();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInOrderVerification() {
    compilationHelper
        .addSourceLines(
            "Lib.java",
            "public class Lib {",
            "  @com.google.errorprone.annotations.CheckReturnValue",
            "  public int f() {",
            "    return 0;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.inOrder;",
            "class Test {",
            "  void m() {",
            "    inOrder().verify(new Lib()).f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreVoidReturningMethodReferences() {
    compilationHelper
        .addSourceLines(
            "Lib.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Lib {",
            "  public static void consume(Object o) {}",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m(java.util.List<Object> xs) {",
            "    xs.forEach(Lib::consume);",
            "  }",
            "}")
        .doTest();
  }

  /** Test class containing a method annotated with @CRV. */
  public static final class CRVTest {
    @com.google.errorprone.annotations.CheckReturnValue
    public static int f() {
      return 42;
    }

    private CRVTest() {}
  }

  @Test
  public void noCRVonClasspath() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    // BUG: Diagnostic contains: ",
            "    com.google.errorprone.bugpatterns.CheckReturnValueTest.CRVTest.f();",
            "  }",
            "}")
        .withClasspath(CRVTest.class, CheckReturnValueTest.class)
        .doTest();
  }

  @Test
  public void constructor() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @com.google.errorprone.annotations.CheckReturnValue",
            "  public Test() {}",
            "  public static void foo() {",
            "    // BUG: Diagnostic contains: ",
            "    new Test();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_telescoping() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @com.google.errorprone.annotations.CheckReturnValue",
            "  public Test() {}",
            "  public Test(int foo) { this(); }",
            "  public static void foo() {",
            "    Test foo = new Test(42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_superCall() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @com.google.errorprone.annotations.CheckReturnValue",
            "  public Test() {}",
            "  static class SubTest extends Test { SubTest() { super(); } }",
            "  public static void foo() {",
            "    Test derived = new SubTest();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_anonymousClassInheritsCIRV() {
    compilationHelperLookingAtAllConstructors()
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  public Test() {}",
            "  public static void foo() {",
            "    new Test() {};",
            "    new Test() {{ System.out.println(\"Lookie, instance initializer\"); }};",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_anonymousClassInheritsCRV() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @com.google.errorprone.annotations.CheckReturnValue",
            "  public Test() {}",
            "  public static void foo() {",
            "    // BUG: Diagnostic contains: ",
            "    new Test() {};",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_hasOuterInstance() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  class Inner {",
            "    @com.google.errorprone.annotations.CheckReturnValue",
            "    public Inner() {}",
            "  }",
            "  public static void foo() {",
            "    // BUG: Diagnostic contains: ",
            "    new Test().new Inner() {};",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_anonymousClassInheritsCRV_syntheticConstructor() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @com.google.errorprone.annotations.CheckReturnValue",
            "  static class Nested {}",
            "  public static void foo() {",
            "    // BUG: Diagnostic contains: ",
            "    new Nested() {};", // The "called" constructor is synthetic, but within @CRV Nested
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_inheritsFromCrvInterface() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @com.google.errorprone.annotations.CheckReturnValue",
            "  static interface IFace {}",
            "  public static void foo() {",
            //  TODO(b/226203690): It's arguable that this might need to be @CRV?
            //   The superclass of the anonymous class is Object, not IFace, but /shrug
            "    new IFace() {};",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_throwingContexts() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Foo {}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      new Foo();",
            "      org.junit.Assert.fail();",
            "    } catch (Exception expected) {}",
            "    org.junit.Assert.assertThrows(IllegalArgumentException.class, () -> new Foo());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_reference() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "public class Foo {}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: ",
            "    Runnable ignoresResult = Foo::new;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_withoutCrvAnnotation() {
    compilationHelperLookingAtAllConstructors()
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public Test() {}",
            "  public static void foo() {",
            "    // BUG: Diagnostic contains: ",
            "    new Test();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void allMethods_withoutCIRVAnnotation() {
    compilationHelperLookingAtAllMethods()
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public int bar() { return 42; }",
            "  public static void foo() {",
            "    // BUG: Diagnostic contains: ",
            "    new Test().bar();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void allMethods_withExternallyConfiguredIgnoreList() {
    compileWithExternalApis("java.util.List#add(java.lang.Object)")
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test {",
            "  public static void foo(List<Integer> x) {",
            "    x.add(42);",
            "    // BUG: Diagnostic contains: ",
            "    x.get(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void usingElementInTestExpected() {
    compilationHelperLookingAtAllConstructors()
        .addSourceLines(
            "Foo.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.junit.Test;",
            "@RunWith(JUnit4.class)",
            "class Foo {",
            "  @Test(expected = IllegalArgumentException.class) ",
            "  public void foo() {",
            "    new Foo();", // OK when it's the only statement
            "  }",
            "  @Test(expected = IllegalArgumentException.class) ",
            "  public void fooWith2Statements() {",
            "    Foo f = new Foo();",
            "    // BUG: Diagnostic contains: ",
            "    new Foo();", // Not OK if there is more than one statement in the block.
            "  }",
            "  @Test(expected = Test.None.class) ", // This is a weird way to spell the default
            "  public void fooWithNone() {",
            "    // BUG: Diagnostic contains: ",
            "    new Foo();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAutoValueBuilderSetterMethods() {
    compilationHelper
        .addSourceLines(
            "Animal.java",
            "package com.google.frobber;",
            "import com.google.auto.value.AutoValue;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@AutoValue",
            "@CheckReturnValue",
            "abstract class Animal {",
            "  abstract String name();",
            "  abstract int numberOfLegs();",
            "  static Builder builder() {",
            "    return new AutoValue_Animal.Builder();",
            "  }",
            "  @AutoValue.Builder",
            "  abstract static class Builder {",
            "    abstract Builder setName(String value);",
            "    abstract Builder setNumberOfLegs(int value);",
            "    abstract Animal build();",
            "  }",
            "}")
        .addSourceLines(
            "AnimalCaller.java",
            "package com.google.frobber;",
            "public final class AnimalCaller {",
            "  static void testAnimal() {",
            "    Animal.Builder builder = Animal.builder();",
            "    builder.setNumberOfLegs(4);", // AutoValue.Builder setters are implicitly @CIRV
            "    // BUG: Diagnostic contains: ",
            "    builder.build();",
            "  }",
            "}")
        .setArgs(ImmutableList.of("-processor", AutoValueProcessor.class.getName()))
        .doTest();
  }

  @Test
  public void testAutoBuilderSetterMethods() {
    compilationHelper
        .addSourceLines(
            "Person.java",
            "package com.google.frobber;",
            "public final class Person {",
            "  public Person(String name, int id) {}",
            "}")
        .addSourceLines(
            "PersonBuilder.java",
            "package com.google.frobber;",
            "import com.google.auto.value.AutoBuilder;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CheckReturnValue",
            "@AutoBuilder(ofClass = Person.class)",
            "abstract class PersonBuilder {",
            "  static PersonBuilder personBuilder() {",
            "    return new AutoBuilder_PersonBuilder();",
            "  }",
            "  abstract PersonBuilder setName(String name);",
            "  abstract PersonBuilder setId(int id);",
            "  abstract Person build();",
            "}")
        .addSourceLines(
            "PersonCaller.java",
            "package com.google.frobber;",
            "public final class PersonCaller {",
            "  static void testPersonBuilder() {",
            "    // BUG: Diagnostic contains: ",
            "    PersonBuilder.personBuilder();",
            "    PersonBuilder builder = PersonBuilder.personBuilder();",
            "    builder.setName(\"kurt\");", // AutoBuilder setters are implicitly @CIRV
            "    builder.setId(42);", // AutoBuilder setters are implicitly @CIRV
            "    // BUG: Diagnostic contains: ",
            "    builder.build();",
            "  }",
            "}")
        .setArgs(ImmutableList.of("-processor", AutoBuilderProcessor.class.getName()))
        .doTest();
  }

  @Test
  public void testAutoBuilderSetterMethods_withInterface() {
    compilationHelper
        .addSourceLines(
            "LogUtil.java",
            "package com.google.frobber;",
            "import java.util.logging.Level;",
            "public class LogUtil {",
            "  public static void log(Level severity, String message) {}",
            "}")
        .addSourceLines(
            "Caller.java",
            "package com.google.frobber;",
            "import com.google.auto.value.AutoBuilder;",
            "import java.util.logging.Level;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CheckReturnValue",
            "@AutoBuilder(callMethod = \"log\", ofClass = LogUtil.class)",
            "public interface Caller {",
            "  static Caller logCaller() {",
            "    return new AutoBuilder_Caller();",
            "  }",
            "  Caller setSeverity(Level level);",
            "  Caller setMessage(String message);",
            "  void call(); // calls: LogUtil.log(severity, message)",
            "}")
        .addSourceLines(
            "LogCaller.java",
            "package com.google.frobber;",
            "import java.util.logging.Level;",
            "public final class LogCaller {",
            "  static void testLogCaller() {",
            "    // BUG: Diagnostic contains: ",
            "    Caller.logCaller();",
            "    Caller caller = Caller.logCaller();",
            "    caller.setMessage(\"hi\");", // AutoBuilder setters are implicitly @CIRV
            "    caller.setSeverity(Level.FINE);", // AutoBuilder setters are implicitly @CIRV
            "    caller.call();",
            "  }",
            "}")
        .setArgs(ImmutableList.of("-processor", AutoBuilderProcessor.class.getName()))
        .doTest();
  }

  @Test
  public void testPackagesRule() {
    compilationHelperWithPackagePatterns("java.util")
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  public static void foo(List<Integer> list, Pattern pattern) {",
            "    // BUG: Diagnostic contains: ",
            "    list.get(0);",
            "    // BUG: Diagnostic contains: ",
            "    pattern.matcher(\"blah\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPackagesRule_negativePattern() {
    compilationHelperWithPackagePatterns("java.util", "-java.util.regex")
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  public static void foo(List<Integer> list, Pattern pattern) {",
            "    // BUG: Diagnostic contains: ",
            "    list.get(0);",
            "    pattern.matcher(\"blah\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPackagesRule_negativePattern_doesNotMakeOptional() {
    // A negative pattern just makes the packages rule itself not apply to that package and its
    // subpackages if it otherwise would because of a positive pattern on a superpackage. It doesn't
    // make APIs in that package CIRV.
    compilationHelperWithPackagePatterns("java.util", "-java.util.regex")
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "import java.util.regex.Pattern;",
            "import java.util.regex.PatternSyntaxException;",
            "class Test {",
            "  public static void foo(List<Integer> list, Pattern pattern) {",
            "    // BUG: Diagnostic contains: ",
            "    list.get(0);",
            "    pattern.matcher(\"blah\");",
            "    // BUG: Diagnostic contains: ",
            "    new PatternSyntaxException(\"\", \"\", 0);",
            "  }",
            "}")
        .doTest();
  }

  private CompilationTestHelper compilationHelperLookingAtAllConstructors() {
    return compilationHelper.setArgs(
        "-XepOpt:" + CheckReturnValue.CHECK_ALL_CONSTRUCTORS + "=true");
  }

  private CompilationTestHelper compilationHelperLookingAtAllMethods() {
    return compilationHelper.setArgs("-XepOpt:" + CheckReturnValue.CHECK_ALL_METHODS + "=true");
  }

  private CompilationTestHelper compileWithExternalApis(String... apis) {
    try {
      Path file = temporaryFolder.newFile().toPath();
      Files.writeString(file, Joiner.on('\n').join(apis), UTF_8);

      return compilationHelper.setArgs(
          "-XepOpt:" + CheckReturnValue.CHECK_ALL_METHODS + "=true",
          "-XepOpt:CheckReturnValue:ApiExclusionList=" + file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private CompilationTestHelper compilationHelperWithPackagePatterns(String... patterns) {
    return compilationHelper.setArgs(
        "-XepOpt:" + CheckReturnValue.CRV_PACKAGES + "=" + Joiner.on(',').join(patterns),
        "-XepOpt:" + CheckReturnValue.CHECK_ALL_CONSTRUCTORS + "=true");
  }
}
