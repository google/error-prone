package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class NonnullParameterTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(NonnullParameter.class, getClass());
  }

  @Test
  public void testPositiveCases() throws Exception {
    compilationHelper.addSourceFile("NonnullParameterPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.addSourceFile("NonnullParameterNegativeCases.java").doTest();
  }

  @Test
  public void testPackageAnnotation() {
    compilationHelper
        .addSourceLines(
            "package-info.java",
            "@javax.annotation.ParametersAreNonnullByDefault",
            "package lib;")
        .addSourceLines(
            "lib/Foo.java",
            "package lib;",
            "public class Foo {",
            "  public static void foo(Object o) {}",
            "}")
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  void m() {",
            "    // BUG: Diagnostic contains: null value",
            "    lib.Foo.foo(null);",
            "  }",
            "}")
        .doTest();
  }

  @Ignore
  @Test
  public void testMultiplePackageAnnotations() {
    compilationHelper
        .addSourceLines(
            "package-info.java",
            "@javax.annotation.ParametersAreNonnullByDefault",
            "@javax.annotation.ParametersAreNullableByDefault",
            "// BUG: Diagnostic contains: More than one",
            "package lib;")
        .doTest();
  }
}
