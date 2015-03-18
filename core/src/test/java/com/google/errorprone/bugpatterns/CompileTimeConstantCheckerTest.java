/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
    compilationHelper = CompilationTestHelper.newInstance(new CompileTimeConstantChecker());
  }

  @Test
  public void testMatches_fieldAccessFailsWithNonConstant() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public void m(String p, @CompileTimeConstant String q) { }",
            "  // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "  public void r(String s) { this.m(\"boo\", s); }",
            "}"));
  }

  @Test
  public void testMatches_fieldAccessFailsWithNonConstantExpression() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public void m(String p, @CompileTimeConstant String q) { }",
            "  // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "  public void r(String s) { this.m(\"boo\", s +\"boo\"); }",
            "}"));
  }

  @Test
  public void testMatches_fieldAccessSucceedsWithLiteral() throws Exception {
    compilationHelper.assertCompileSucceeds(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public void m(String s, @CompileTimeConstant String p) { }",
            "  public void r(String x) { this.m(x, \"boo\"); }",
            "}"));
  }

  @Test
  public void testMatches_fieldAccessSucceedsWithStaticFinal() throws Exception {
    compilationHelper.assertCompileSucceeds(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static final String S = \"Hello\";",
            "  public void m(String s, @CompileTimeConstant String p) { }",
            "  public void r(String x) { this.m(x, S); }",
            "}"));
  }

  @Test
  public void testMatches_fieldAccessSucceedsWithConstantConcatenation() throws Exception {
    compilationHelper.assertCompileSucceeds(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static final String S = \"Hello\";",
            "  public void m(String s, @CompileTimeConstant String p) { }",
            "  public void r(String x) { this.m(x, S + \" World!\"); }",
            "}"));
  }

  @Test
  public void testMatches_identCallFailsWithNonConstant() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public void m(@CompileTimeConstant String p, int i) { }",
            "  // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "  public void r(String s) { m(s, 19); }",
            "}"));
  }

  @Test
  public void testMatches_identCallSucceedsWithLiteral() throws Exception {
    compilationHelper.assertCompileSucceeds(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public void m(String s, @CompileTimeConstant String p) { }",
            "  public void r(@CompileTimeConstant final String x) { m(x, x); }",
            "  public void s() { r(\"boo\"); }",
            "}"));
  }

  @Test
  public void testMatches_staticCallFailsWithNonConstant() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static void m(@CompileTimeConstant String p, int i) { }",
            "  // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "  public static void r(String s) { m(s, 19); }",
            "}"));
  }

  @Test
  public void testMatches_staticCallSucceedsWithLiteral() throws Exception {
    compilationHelper.assertCompileSucceeds(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static void m(String s, @CompileTimeConstant String p) { }",
            "  public static void r(String x) { m(x, \"boo\"); }",
            "}"));
  }

  @Test
  public void testMatches_qualifiedStaticCallFailsWithNonConstant() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static class Inner {",
            "    public static void m(@CompileTimeConstant String p, int i) { }",
            "  }",
            "  // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "  public static void r(String s) { Inner.m(s, 19); }",
            "}"));
  }

  @Test
  public void testMatches_qualifiedStaticCallSucceedsWithLiteral() throws Exception {
    compilationHelper.assertCompileSucceeds(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static class Inner {",
            "    public static void m(String s, @CompileTimeConstant String p) { }",
            "  }",
            "  public static void r(String x) { Inner.m(x, \"boo\"); }",
            "}"));
  }

  @Test
  public void testMatches_ctorSucceedsWithLiteral() throws Exception {
    compilationHelper.assertCompileSucceeds(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public CompileTimeConstantTestCase(",
            "      String s, @CompileTimeConstant String p) { }",
            "  public static CompileTimeConstantTestCase makeNew(String x) {",
            "    return new CompileTimeConstantTestCase(x, \"boo\");",
            "  }",
            "}"));
  }

  @Test
  public void testMatches_ctorFailsWithNonConstant() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public CompileTimeConstantTestCase(",
            "      String s, @CompileTimeConstant String p) { }",
            "  public static CompileTimeConstantTestCase makeNew(String x) {",
            "    // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "    return new CompileTimeConstantTestCase(\"boo\", x);",
            "  }",
            "}"));
  }

  @Test
  public void testMatches_identCallSucceedsWithinCtorWithLiteral() throws Exception {
    compilationHelper.assertCompileSucceeds(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public CompileTimeConstantTestCase(",
            "      String s, @CompileTimeConstant final String p) { m(p); }",
            "  public void m(@CompileTimeConstant String r) {}",
            "  public static CompileTimeConstantTestCase makeNew(String x) {",
            "    return new CompileTimeConstantTestCase(x, \"boo\");",
            "  }",
            "}"));
  }

  @Test
  public void testMatches_varargsFail() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static void m(String s, @CompileTimeConstant String... p) { }",
            "  // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "  public static void r(String s) { m(s, \"foo\", s); }",
            "}"));
  }

  @Test
  public void testMatches_varargsSuccess() throws Exception {
    compilationHelper.assertCompileSucceeds(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
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
            "}"));
  }

  @Test
  public void testMatches_nonFinalCompileTimeConstantParam() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().forSourceLines("test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  public static void m(@CompileTimeConstant String y) { }",
            "  public static void r(@CompileTimeConstant String x) { ",
            "    // BUG: Diagnostic contains: . Did you mean to make 'x' final?",
            "    m(x); ",
            "  }",
            "}"));
  }
}
