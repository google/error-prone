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

/** {@link CompileTimeConstantChecker}Test */
@RunWith(JUnit4.class)
public class CompileTimeConstantCheckerTest {

  public static final String ERROR_MESSAGE =
      "[CompileTimeConstant] Non-compile-time constant expression passed "
          + "to parameter with @CompileTimeConstant type annotation";

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(CompileTimeConstantChecker.class, getClass());
  }

  @Test
  public void test_SuppressWarningsDoesntWork() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static void m(@CompileTimeConstant String s) { }",
            "  @SuppressWarnings(\"CompileTimeConstant\")",
            " // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "  public static void r(String x) { m(x); }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_fieldAccessFailsWithNonConstant() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public void m(String p, @CompileTimeConstant String q) { }",
            "  // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "  public void r(String s) { this.m(\"boo\", s); }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_fieldAccessFailsWithNonConstantExpression() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public void m(String p, @CompileTimeConstant String q) { }",
            "  // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "  public void r(String s) { this.m(\"boo\", s +\"boo\"); }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_fieldAccessSucceedsWithLiteral() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public void m(String s, @CompileTimeConstant String p) { }",
            "  public void r(String x) { this.m(x, \"boo\"); }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_fieldAccessSucceedsWithStaticFinal() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static final String S = \"Hello\";",
            "  public void m(String s, @CompileTimeConstant String p) { }",
            "  public void r(String x) { this.m(x, S); }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_fieldAccessSucceedsWithConstantConcatenation() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static final String S = \"Hello\";",
            "  public void m(String s, @CompileTimeConstant String p) { }",
            "  public void r(String x) { this.m(x, S + \" World!\"); }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_identCallFailsWithNonConstant() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public void m(@CompileTimeConstant String p, int i) { }",
            "  // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "  public void r(String s) { m(s, 19); }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_identCallSucceedsWithLiteral() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public void m(String s, @CompileTimeConstant String p) { }",
            "  public void r(@CompileTimeConstant final String x) { m(x, x); }",
            "  public void s() { r(\"boo\"); }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_staticCallFailsWithNonConstant() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static void m(@CompileTimeConstant String p, int i) { }",
            "  // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "  public static void r(String s) { m(s, 19); }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_staticCallSucceedsWithLiteral() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static void m(String s, @CompileTimeConstant String p) { }",
            "  public static void r(String x) { m(x, \"boo\"); }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_qualifiedStaticCallFailsWithNonConstant() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static class Inner {",
            "    public static void m(@CompileTimeConstant String p, int i) { }",
            "  }",
            "  // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "  public static void r(String s) { Inner.m(s, 19); }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_qualifiedStaticCallSucceedsWithLiteral() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static class Inner {",
            "    public static void m(String s, @CompileTimeConstant String p) { }",
            "  }",
            "  public static void r(String x) { Inner.m(x, \"boo\"); }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_ctorSucceedsWithLiteral() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public CompileTimeConstantTestCase(",
            "      String s, @CompileTimeConstant String p) { }",
            "  public static CompileTimeConstantTestCase makeNew(String x) {",
            "    return new CompileTimeConstantTestCase(x, \"boo\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_ctorFailsWithNonConstant() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public CompileTimeConstantTestCase(",
            "      String s, @CompileTimeConstant String p) { }",
            "  public static CompileTimeConstantTestCase makeNew(String x) {",
            "    // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "    return new CompileTimeConstantTestCase(\"boo\", x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_identCallSucceedsWithinCtorWithLiteral() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public CompileTimeConstantTestCase(",
            "      String s, @CompileTimeConstant final String p) { m(p); }",
            "  public void m(@CompileTimeConstant String r) {}",
            "  public static CompileTimeConstantTestCase makeNew(String x) {",
            "    return new CompileTimeConstantTestCase(x, \"boo\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_varargsFail() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static void m(String s, @CompileTimeConstant String... p) { }",
            "  // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "  public static void r(String s) { m(s, \"foo\", s); }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_varargsSuccess() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static void m(String s, @CompileTimeConstant String... p) { }",
            "  public static void r(String s) { ",
            "    m(s); ",
            "    m(s, \"foo\"); ",
            "    m(s, \"foo\", \"bar\"); ",
            "    m(s, \"foo\", \"bar\", \"baz\"); ",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_effectivelyFinalCompileTimeConstantParam() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static void m(@CompileTimeConstant String y) { }",
            "  public static void r(@CompileTimeConstant String x) { ",
            "    m(x); ",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_nonFinalCompileTimeConstantParam() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static void m(@CompileTimeConstant String y) { }",
            "  public static void r(@CompileTimeConstant String x) { ",
            "    x = x + \"!\";",
            "    // BUG: Diagnostic contains: . Did you mean to make 'x' final?",
            "    m(x); ",
            "  }",
            "}")
        .doTest();
  }
}
