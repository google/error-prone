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

import com.google.common.io.ByteStreams;
import com.google.errorprone.CompilationTestHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author eaftan@google.com (Eddie Aftandilian) */
@RunWith(JUnit4.class)
public class CheckReturnValueTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(CheckReturnValue.class, getClass());
  }

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
            "@javax.annotation.CheckReturnValue",
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
        .addSourceLines("package-info.java", "@javax.annotation.CheckReturnValue", "package lib;")
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
            "    // BUG: Diagnostic contains: Ignored return value",
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
            "@javax.annotation.CheckReturnValue",
            "public class Lib {",
            "  public static int f() { return 42; }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    // BUG: Diagnostic contains: Ignored return value",
            "    lib.Lib.f();",
            "  }",
            "}")
        .doTest();
  }

  // Don't match void-returning methods in packages with @CRV
  @Test
  public void testVoidReturningMethodInAnnotatedPackage() {
    compilationHelper
        .addSourceLines("package-info.java", "@javax.annotation.CheckReturnValue", "package lib;")
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "public class Lib {",
            "  public static void f() {}",
            "}")
        .addSourceLines("Test.java", "class Test {", "  void m() {", "    lib.Lib.f();", "  }", "}")
        .doTest();
  }

  @Test
  public void badCRVOnProcedure() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package lib;",
            "@javax.annotation.CheckReturnValue",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  // @CheckReturnValue may not be applied to void-returning methods",
            "  @javax.annotation.CheckReturnValue public static void f() {}",
            "}")
        .doTest();
  }

  @Test
  public void badCRVOnPseudoProcedure() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package lib;",
            "@javax.annotation.CheckReturnValue",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  // @CheckReturnValue may not be applied to void-returning methods",
            "  @javax.annotation.CheckReturnValue public static Void f() {",
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
            "  @javax.annotation.CheckReturnValue",
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
        .addSourceLines("package-info.java", "@javax.annotation.CheckReturnValue", "package lib;")
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "public class Lib {",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  public static int f() { return 42; }",
            "}")
        .addSourceLines("Test.java", "class Test {", "  void m() {", "    lib.Lib.f();", "  }", "}")
        .doTest();
  }

  @Test
  public void testClassAnnotationButCanIgnoreReturnValue() {
    compilationHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "@javax.annotation.CheckReturnValue",
            "public class Lib {",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  public static int f() { return 42; }",
            "}")
        .addSourceLines("Test.java", "class Test {", "  void m() {", "    lib.Lib.f();", "  }", "}")
        .doTest();
  }

  @Test
  public void badCanIgnoreReturnValueOnProcedure() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package lib;",
            "@javax.annotation.CheckReturnValue",
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
            "@javax.annotation.CheckReturnValue",
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
            "    // BUG: Diagnostic contains: Ignored return value",
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
            "@javax.annotation.CheckReturnValue",
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
        .addSourceLines("package-info.java", "@javax.annotation.CheckReturnValue", "package lib;")
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "@com.google.errorprone.annotations.CanIgnoreReturnValue",
            "public class Lib {",
            "  public static int f() { return 42; }",
            "}")
        .addSourceLines("Test.java", "class Test {", "  void m() {", "    lib.Lib.f();", "  }", "}")
        .doTest();
  }

  @Test
  public void errorBothClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "@com.google.errorprone.annotations.CanIgnoreReturnValue",
            "@javax.annotation.CheckReturnValue",
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
            "  @javax.annotation.CheckReturnValue",
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
        .addSourceLines("package-info.java", "@javax.annotation.CheckReturnValue", "package lib;")
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "public class Lib {",
            "  public static Void f() {",
            "    return null;",
            "  }",
            "}")
        .addSourceLines("Test.java", "class Test {", "  void m() {", "    lib.Lib.f();", "  }", "}")
        .doTest();
  }

  @Test
  public void ignoreInTests() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "@javax.annotation.CheckReturnValue",
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
            "@javax.annotation.CheckReturnValue",
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
            "@javax.annotation.CheckReturnValue",
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
            "@javax.annotation.CheckReturnValue",
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
            "     // BUG: Diagnostic contains: Ignored return value",
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
            "@javax.annotation.CheckReturnValue",
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
            "@javax.annotation.CheckReturnValue",
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
            "    // BUG: Diagnostic contains: Ignored return value",
            "    foo.f();",
            "    org.junit.Assert.fail();",
            "    // BUG: Diagnostic contains: Ignored return value",
            "    foo.f();",
            "    junit.framework.Assert.fail();",
            "    // BUG: Diagnostic contains: Ignored return value",
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
            "  @javax.annotation.CheckReturnValue",
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

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  /** Test class containing a method annotated with @CRV. */
  public static class CRVTest {
    @javax.annotation.CheckReturnValue
    public static int f() {
      return 42;
    }
  }

  static void addClassToJar(JarOutputStream jos, Class<?> clazz) throws IOException {
    String entryPath = clazz.getName().replace('.', '/') + ".class";
    try (InputStream is = clazz.getClassLoader().getResourceAsStream(entryPath)) {
      jos.putNextEntry(new JarEntry(entryPath));
      ByteStreams.copy(is, jos);
    }
  }

  @Test
  public void noCRVonClasspath() throws Exception {
    File libJar = tempFolder.newFile("lib.jar");
    try (FileOutputStream fis = new FileOutputStream(libJar);
        JarOutputStream jos = new JarOutputStream(fis)) {
      addClassToJar(jos, CRVTest.class);
      addClassToJar(jos, CheckReturnValueTest.class);
    }
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    // BUG: Diagnostic contains: Ignored return value",
            "    com.google.errorprone.bugpatterns.CheckReturnValueTest.CRVTest.f();",
            "  }",
            "}")
        .setArgs(Arrays.asList("-cp", libJar.toString()))
        .doTest();
  }
}
