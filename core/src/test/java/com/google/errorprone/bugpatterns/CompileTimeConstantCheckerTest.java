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
import com.google.errorprone.annotations.CompileTimeConstant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link CompileTimeConstantChecker}Test */
@RunWith(JUnit4.class)
public class CompileTimeConstantCheckerTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(CompileTimeConstantChecker.class, getClass());

  @Test
  public void matches_fieldAccessFailsWithNonConstant() {
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
  public void matches_fieldAccessFailsWithNonConstantExpression() {
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
  public void matches_fieldAccessSucceedsWithLiteral() {
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
  public void matches_fieldAccessSucceedsWithStaticFinal() {
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
  public void matches_fieldAccessSucceedsWithConstantConcatenation() {
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
  public void matches_identCallFailsWithNonConstant() {
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
  public void matches_identCallSucceedsWithLiteral() {
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
  public void matches_staticCallFailsWithNonConstant() {
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
  public void matches_staticCallSucceedsWithLiteral() {
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
  public void matches_qualifiedStaticCallFailsWithNonConstant() {
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
  public void matches_qualifiedStaticCallSucceedsWithLiteral() {
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
  public void matches_ctorSucceedsWithLiteral() {
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
  public void matches_ctorFailsWithNonConstant() {
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
  public void matches_identCallSucceedsWithinCtorWithLiteral() {
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

  /** Holder for a method we wish to reference from a test. */
  public static class Holder {
    public static void m(String s, @CompileTimeConstant String... p) {}

    private Holder() {}
  }

  @Test
  public void matches_varargsInDifferentCompilationUnit() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "import " + Holder.class.getCanonicalName() + ";",
            "public class CompileTimeConstantTestCase {",
            "  public static void r(String s) {",
            "    // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "    Holder.m(s, \"foo\", s);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void matches_varargsSuccess() {
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
  public void matches_effectivelyFinalCompileTimeConstantParam() {
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
  public void matches_nonFinalCompileTimeConstantParam() {
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

  @Test
  public void matches_override() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "abstract class CompileTimeConstantTestCase {",
            "  abstract void m(String y);",
            "  static class C extends CompileTimeConstantTestCase {",
            "    // BUG: Diagnostic contains: Method with @CompileTimeConstant parameter",
            "    @Override void m(@CompileTimeConstant String s) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void matches_methodReference() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "import java.util.function.Consumer;",
            "public class CompileTimeConstantTestCase {",
            "  public static void m(@CompileTimeConstant String s) { }",
            "  public static Consumer<String> r(String x) {",
            "    // BUG: Diagnostic contains: Method with @CompileTimeConstant parameter",
            "    return CompileTimeConstantTestCase::m;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void matches_constructorReference() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "import java.util.function.Function;",
            "public class CompileTimeConstantTestCase {",
            "  CompileTimeConstantTestCase(@CompileTimeConstant String s) { }",
            "  public static Function<String, CompileTimeConstantTestCase> r(String x) {",
            "    // BUG: Diagnostic contains: Method with @CompileTimeConstant parameter",
            "    return CompileTimeConstantTestCase::new;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void matches_methodReferenceCorrectOverrideMethod() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "import java.util.function.Consumer;",
            "public class CompileTimeConstantTestCase {",
            "  interface ConstantFn {",
            "    void apply(@CompileTimeConstant String s);",
            "  }",
            "  public static void m(@CompileTimeConstant String s) { }",
            "  public static ConstantFn r(String x) {",
            "    return CompileTimeConstantTestCase::m;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void matches_methodReferenceCorrectOverrideConstructor() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "import java.util.function.Consumer;",
            "public class CompileTimeConstantTestCase {",
            "  interface ConstantFn {",
            "    CompileTimeConstantTestCase apply(@CompileTimeConstant String s);",
            "  }",
            "  CompileTimeConstantTestCase(@CompileTimeConstant String s) {}",
            "  public static ConstantFn r(String x) {",
            "    return CompileTimeConstantTestCase::new;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void matches_lambdaExpression() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "import java.util.function.Consumer;",
            "public class CompileTimeConstantTestCase {",
            "  // BUG: Diagnostic contains: Method with @CompileTimeConstant parameter",
            "  Consumer<String> c = (@CompileTimeConstant String s) -> {};",
            "}")
        .doTest();
  }

  @Test
  public void doesNotMatch_lambdaExpression_correctOverride() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "import java.util.function.Consumer;",
            "public class CompileTimeConstantTestCase {",
            "  interface ConstantFn {",
            "    void apply(@CompileTimeConstant String s);",
            "  }",
            "  ConstantFn c = (@CompileTimeConstant String s) -> {doFoo(s);};",
            "  void doFoo(final @CompileTimeConstant String foo) {}",
            "}")
        .doTest();
  }

  @Test
  public void matches_lambdaExpressionWithoutAnnotatedParameters() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "import java.util.function.Consumer;",
            "public class CompileTimeConstantTestCase {",
            "  interface ConstantFn {",
            "    void apply(@CompileTimeConstant String s);",
            "  }",
            "  // BUG: Diagnostic contains: Non-compile-time constant expression",
            "  ConstantFn c = s -> {doFoo(s);};",
            "  void doFoo(final @CompileTimeConstant String foo) {}",
            "}")
        .doTest();
  }

  @Test
  public void matches_lambdaExpressionWithoutExplicitFormalParameters() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  @FunctionalInterface",
            "  interface I {",
            "    void f(@CompileTimeConstant String x);",
            "  }",
            "  void f(String s) {",
            "    I i = x -> {};",
            "    // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "    i.f(s);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void reportsDiagnostic_whenConstantFieldDeclaredWithoutFinal() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  // BUG: Diagnostic contains: . Did you mean to make 's' final?",
            "  @CompileTimeConstant String s = \"s\";",
            "}")
        .doTest();
  }

  @Test
  public void noDiagnostic_whenConstantFieldDeclaredFinal() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  @CompileTimeConstant final String s = \"s\";",
            "}")
        .doTest();
  }

  @Test
  public void reportsDiagnostic_whenInitialisingFinalFieldWithNonConstant() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  @CompileTimeConstant final String s;",
            "  CompileTimeConstantTestCase(String s) {",
            "    // BUG: Diagnostic contains: Non-compile-time constant expression",
            "    this.s = s;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void noDiagnostic_whenInitialisingFinalFieldWithConstant() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  @CompileTimeConstant final String s;",
            "  CompileTimeConstantTestCase(@CompileTimeConstant String s) {",
            "    this.s = s;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void noDiagnostic_whenInvokingMethodWithFinalField() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class CompileTimeConstantTestCase {",
            "  @CompileTimeConstant final String s;",
            "  CompileTimeConstantTestCase(@CompileTimeConstant String s) {",
            "    this.s = s;",
            "  }",
            "  void invokeCTCMethod() {",
            "    ctcMethod(s);",
            "  }",
            "  void ctcMethod(@CompileTimeConstant String s) {}",
            "}")
        .doTest();
  }

  @Test
  public void reportsDiagnostic_whenConstantEnumFieldDeclaredWithoutFinal() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public enum Test {",
            "  A(\"A\");",
            "  // BUG: Diagnostic contains: . Did you mean to make 's' final?",
            "  @CompileTimeConstant String s;",
            "  Test(@CompileTimeConstant String s) {",
            "    this.s = s;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void noDiagnostic_whenConstantEnumFieldDeclaredFinal() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public enum Test {",
            "  A(\"A\");",
            "  @CompileTimeConstant final String s;",
            "  Test(@CompileTimeConstant String s) {",
            "    this.s = s;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void reportsDiagnostic_whenInitialisingFinalEnumFieldWithNonConstant() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public enum Test {",
            "  A(\"A\");",
            "  @CompileTimeConstant final String s;",
            "  Test(String s) {",
            "    // BUG: Diagnostic contains: Non-compile-time constant expression",
            "    this.s = s;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void noDiagnostic_whenInvokingMethodWithFinalEnumField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public enum Test {",
            "  A(\"A\");",
            "  @CompileTimeConstant final String s;",
            "  Test(@CompileTimeConstant String s) {",
            "    this.s = s;",
            "  }",
            "  void invokeCTCMethod() {",
            "    ctcMethod(s);",
            "  }",
            "  void ctcMethod(@CompileTimeConstant String s) {}",
            "}")
        .doTest();
  }

  @Test
  public void nonConstantField_positive() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public abstract class CompileTimeConstantTestCase {",
            "  abstract String something();",
            "  // BUG: Diagnostic contains: Non-compile-time constant expression",
            "  @CompileTimeConstant final String x = something();",
            "}")
        .doTest();
  }

  @Test
  public void constantField_immutableList() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public abstract class CompileTimeConstantTestCase {",
            "  @CompileTimeConstant final ImmutableList<String> x = ImmutableList.of(\"a\");",
            "}")
        .doTest();
  }
}
