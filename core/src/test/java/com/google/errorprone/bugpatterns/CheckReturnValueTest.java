/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class CheckReturnValueTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(CheckReturnValue.class, getClass());
  }

  @Test
  public void testPositiveCases() throws Exception {
    compilationHelper.addSourceFile("CheckReturnValuePositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.addSourceFile("CheckReturnValueNegativeCases.java").doTest();
  }

  @Test
  public void testPackageAnnotation() throws Exception {
    compilationHelper
        .addSourceLines(
            "package-info.java",
            "@javax.annotation.CheckReturnValue",
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
            "    // BUG: Diagnostic contains: Ignored return value",
            "    lib.Lib.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testClassAnnotation() throws Exception {
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
  public void testVoidReturningMethodInAnnotatedPackage() throws Exception {
    compilationHelper
        .addSourceLines(
            "package-info.java",
            "@javax.annotation.CheckReturnValue",
            "package lib;")
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "public class Lib {",
            "  public static void f() {}",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    lib.Lib.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void badCRVOnProcedure() throws Exception {
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
  public void badCRVOnPseudoProcedure() throws Exception {
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
  public void testIgnoreCRVOnMockito() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package lib;",
            "public class Test {",
            "  @javax.annotation.CheckReturnValue",
            " public static int f() {",
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
  public void testPackageAnnotationButCanIgnoreReturnValue() throws Exception {
    compilationHelper
        .addSourceLines(
            "package-info.java",
            "@javax.annotation.CheckReturnValue",
            "package lib;")
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "public class Lib {",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  public static int f() { return 42; }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    lib.Lib.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testClassAnnotationButCanIgnoreReturnValue() throws Exception {
    compilationHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "@javax.annotation.CheckReturnValue",
            "public class Lib {",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  public static int f() { return 42; }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    lib.Lib.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void badCanIgnoreReturnValueOnProcedure() throws Exception {
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
  public void testNestedClassAnnotation() throws Exception {
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
  public void testNestedClassWithCanIgnoreAnnotation() throws Exception {
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
        .addSourceLines("Test.java",
            "class Test {",
            "  void m() {",
            "    lib.Lib.Inner.InnerMost.f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPackageWithCanIgnoreAnnotation() throws Exception {
      compilationHelper
          .addSourceLines(
              "package-info.java",
              "@javax.annotation.CheckReturnValue",
              "package lib;")
          .addSourceLines("lib/Lib.java",
              "package lib;",
              "@com.google.errorprone.annotations.CanIgnoreReturnValue",
              "public class Lib {",
              "  public static int f() { return 42; }",
              "}")
          .addSourceLines("Test.java",
              "class Test {",
              "  void m() {",
              "    lib.Lib.f();",
              "  }",
              "}")
          .doTest();
    }

  @Test
  public void errorBothClass() throws Exception {
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
  public void errorBothMethod() throws Exception {
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
  public void testJavaLangVoidReturningMethodInAnnotatedPackage() throws Exception {
    compilationHelper
        .addSourceLines(
            "package-info.java",
            "@javax.annotation.CheckReturnValue",
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
            "Test.java",
            "class Test {",
            "  void m() {",
            "    lib.Lib.f();",
            "  }",
            "}")
        .doTest();
  }
}
