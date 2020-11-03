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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ParameterName}Test */
@RunWith(JUnit4.class)
public class ParameterNameTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(ParameterName.class, getClass());

  @Test
  public void positive() {
    BugCheckerRefactoringTestHelper.newInstance(new ParameterName(), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f(int foo, int bar) {}",
            "  {",
            "    f(/* bar= */ 1, /* foo= */ 2);",
            "    f(/** bar= */ 3, /** foo= */ 4);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f(int foo, int bar) {}",
            "  {",
            "    f(/* foo= */ 1, /* bar= */ 2);",
            "    f(/* foo= */ 3, /* bar= */ 4);",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int foo, int bar) {}",
            "  {",
            "    f(/* foo= */ 1, 2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void issue781() {
    testHelper
        .addSourceLines(
            "a/Baz.java",
            "package a.b;",
            "import a.AbstractFoo;",
            "class Baz extends AbstractFoo {",
            "  @Override",
            "  protected String getFoo() {",
            "    return \"foo\";",
            "  }",
            "}")
        .addSourceLines(
            "a/AbstractFoo.java",
            "package a;",
            "import java.util.function.Function;",
            "class Bar {",
            "  private final Function<String, String> args;",
            "  public Bar(Function<String, String> args) {",
            "    this.args = args;",
            "  }",
            "}",
            "public abstract class AbstractFoo {",
            "  protected abstract String getFoo();",
            "  private String getCommandArguments(String parameters) {",
            "    return null;",
            "  }",
            "  public AbstractFoo() {",
            "    new Bar(this::getCommandArguments);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void issue792() {
    testHelper
        .addSourceLines(
            "a/Foo.java",
            "package a;",
            "class Bar {",
            "}",
            "public class Foo {",
            "  public void setInteger(Integer i) {",
            "  }",
            "  public void callSetInteger() {",
            "    setInteger(0);",
            " }",
            "}")
        .addSourceLines("a/Baz.java", "package a;", "public class Baz extends Foo {", "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_ignoresCall_withNoComments() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object arg1, Object arg2) {",
            "    target(arg1, arg2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_findsError_withOneBadComment() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object arg1, Object arg2) {",
            "    // BUG: Diagnostic contains: 'target(/* param1= */arg1, arg2);'",
            "    target(/* param2= */arg1, arg2);",
            "  }",
            "}")
        .doTest();
  }

  @Test // not allowed by Google style guide, but other styles may want this
  public void namedParametersChecker_findsError_withUnusualIdentifier() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object $param$);",
            "  void test(Object arg) {",
            "    // BUG: Diagnostic contains: 'target(/* $param$= */arg);'",
            "    target(/* param= */arg);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_suggestsSwap_withSwappedArgs() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object arg1, Object arg2) {",
            "    // BUG: Diagnostic contains: 'target(/* param1= */arg2",
            "    target(/* param2= */arg2, /* param1= */arg1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_suggestsSwap_withOneCommentedSwappedArgs() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object arg1, Object arg2) {",
            "    // BUG: Diagnostic contains: 'target(/* param1= */arg2, arg1);'",
            "    target(/* param2= */arg2, arg1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_toleratesApproximateComment_onRequiredNamesMethod() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  void test(Object arg) {",
            "    target(/*note param = */arg);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_tolerateComment_withNoEquals() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  void test(Object arg) {",
            "    target(/*param*/arg);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_toleratesMatchingComment_blockAfter() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  void test(Object arg) {",
            "    target(arg/*param*/);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_toleratesApproximateComment_blockAfter() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  void test(Object arg) {",
            "    target(arg/*imprecise match for param*/);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_toleratesMatchingComment_lineAfter() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  void test(Object arg) {",
            "    target(arg); //param",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_ignoresComment_nonMatchinglineAfter() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  void test(Object arg) {",
            "    target(arg); // some_other_comment",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_ignoresComment_markedUpDelimiter() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object arg1, Object arg2) {",
            "    target(arg1,",
            "    /* ---- param1 <-> param2 ---- */",
            "           arg2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_ignoresComment_wrongNameWithNoEquals() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  void test(Object arg) {",
            "    target(/* some_other_comment */arg);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_ignoresComment_wrongVarargs() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object... param);",
            "  void test(Object arg) {",
            "    target(/* param.!.= */arg);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_matchesComment_withChainedMethod() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract Test getTest(Object param);",
            "  abstract void target(Object param2);",
            "  void test(Object arg, Object arg2) {",
            "    getTest(/* param= */arg).target(arg2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void namedParametersChecker_suggestsChangeComment_whenNoMatchingNames() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object arg1, Object arg2) {",
            "    // BUG: Diagnostic contains:",
            "    // target(/* param1= */arg1, arg2)",
            "    // `/* notMatching= */` does not match formal parameter name `param1`",
            "    target(/* notMatching= */arg1, arg2);",
            "  }",
            "}")
        .doTest();
  }

  /** A test for inner class constructor parameter names across compilation boundaries. */
  public static class InnerClassTest {
    /** An inner class. */
    public class Inner {
      public Inner(int foo, int bar) {}

      // this is a (non-static) inner class on purpose
      {
        System.err.println(InnerClassTest.this);
      }
    }
  }

  @Test
  public void innerClassNegative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import " + InnerClassTest.class.getCanonicalName() + ";",
            "class Test {",
            "  {",
            "    new InnerClassTest().new Inner(/* foo= */ 1, /* bar= */ 2);",
            "  }",
            "}")
        .doTest();
  }

  @Ignore // see b/64954766
  @Test
  public void innerClassPositive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import " + InnerClassTest.class.getCanonicalName() + ";",
            "class Test {",
            "  {",
            "    // BUG: Diagnostic contains:",
            "    new InnerClassTest().new Inner(/* bar= */ 1, /* foo= */ 2);",
            "  }",
            "}")
        .doTest();
  }

  /** A test for anonymous class constructors across compilation boundaries. */
  public static class Foo {
    public Foo(int foo, int bar) {}
  }

  @Test
  public void anonymousClassConstructorNegative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import " + Foo.class.getCanonicalName() + ";",
            "class Test {",
            "  {",
            "    new Foo(/* foo= */ 1, /* bar= */ 2) {",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Ignore // see b/65065109
  @Test
  public void anonymousClassConstructor() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import " + Foo.class.getCanonicalName() + ";",
            "class Test {",
            "  {",
            "    // BUG: Diagnostic contains:",
            "    new Foo(/* bar= */ 1, /* foo= */ 2) {",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void internalAnnotatedParameterNegative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public static class AnnotatedParametersTestClass {",
            "    public @interface Annotated {}",
            "    public static void target(@Annotated int foo) {}",
            "  }",
            "  void test() {",
            "    AnnotatedParametersTestClass.target(/* foo= */ 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void internalAnnotatedParameterPositive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public static class AnnotatedParametersTestClass {",
            "    public @interface Annotated {}",
            "    public static void target(@Annotated int foo) {}",
            "  }",
            "  void test() {",
            "    // BUG: Diagnostic contains: target(/* foo= */ 1)",
            "    AnnotatedParametersTestClass.target(/* bar= */ 1);",
            "  }",
            "}")
        .doTest();
  }

  /** A test for annotated parameters across compilation boundaries. */
  public static class AnnotatedParametersTestClass {
    /** An annotation to apply to method parameters. */
    public @interface Annotated {}

    public static void target(@Annotated int foo) {}
  }

  @Test
  public void externalAnnotatedParameterNegative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import " + AnnotatedParametersTestClass.class.getCanonicalName() + ";",
            "class Test {",
            "  void test() {",
            "    AnnotatedParametersTestClass.target(/* foo= */ 1);",
            "  }",
            "}")
        .doTest();
  }

  @Ignore // TODO(b/67993065): remove @Ignore after the issue is fixed.
  @Test
  public void externalAnnotatedParameterPositive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import " + AnnotatedParametersTestClass.class.getCanonicalName() + ";",
            "class Test {",
            "  void test() {",
            "    // BUG: Diagnostic contains: target(/* foo= */ 1)",
            "    AnnotatedParametersTestClass.target(/* bar= */ 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveVarargs() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void foo(int... args) {}",
            "",
            "  void bar() {",
            "    // BUG: Diagnostic contains: /* args...= */",
            "    // /* argh */",
            "    foo(/* argh...= */ 1, 2, 3);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void emptyVarargs_shouldNotCrash() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void foo(int first, int... rest) {}",
            "",
            "  void bar() {",
            "    foo(/* first= */ 1);",
            " // BUG: Diagnostic contains: /* first= */",
            "    foo(/* second= */ 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeVarargs() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void foo(int... args) {}",
            "",
            "  void bar() {",
            "    foo(/* args...= */ 1, 2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varargsCommentAllowedWithArraySyntax() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void foo(int... args) {}",
            "",
            "  void bar() {",
            "    int[] myInts = {1, 2, 3};",
            "    foo(/* args...= */ myInts);",
            "  }",
            "}")
        .doTest();
  }

  // TODO(b/144728869): clean up existing usages with non-"..." syntax
  @Test
  public void normalCommentNotAllowedWithVarargsArraySyntax() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void foo(int... args) {}",
            "",
            "  void bar() {",
            "    int[] myInts = {1, 2, 3};",
            "    // BUG: Diagnostic contains: /* args...= */",
            "    foo(/* args= */ myInts);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varargsCommentAllowedOnOnlyFirstArg() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void foo(int... args) {}",
            "",
            "  void bar() {",
            "    // BUG: Diagnostic contains: parameter name comment only allowed on first varargs"
                + " argument",
            "    foo(1, /* args...= */ 2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varargsWrongFormat() {
    BugCheckerRefactoringTestHelper.newInstance(new ParameterName(), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void foo(int... args) {}",
            "",
            "  void bar() {",
            "    foo(/* args= */ 1, 2);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void foo(int... args) {}",
            "",
            "  void bar() {",
            "    foo(/* args...= */ 1, 2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varargsTrailing() {
    BugCheckerRefactoringTestHelper.newInstance(new ParameterName(), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void foo(int... args) {}",
            "",
            "  void bar() {",
            "    foo(1, /* foo= */ 2);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void foo(int... args) {}",
            "",
            "  void bar() {",
            "    foo(1, /* foo */ 2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varargsIgnoreNonParameterNameComments() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void foo(int... args) {}",
            "",
            "  void bar() {",
            "    foo(/* fake */ 1, 2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varargsWrongNameAndWrongFormat() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void foo(int... args) {}",
            "",
            "  void bar() {",
            "    // BUG: Diagnostic contains: /* args...= */",
            "    // /* argh */",
            "    foo(/* argh= */ 1, 2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varargsCommentNotAllowedOnNormalArg() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void foo(int i) {}",
            "",
            "  void bar() {",
            "    // BUG: Diagnostic contains: /* i= */",
            "    foo(/* i...= */ 1);",
            "  }",
            "}")
        .doTest();
  }

  /** A test input for separate compilation. */
  public static class Holder {
    public static void varargsMethod(int... values) {}
  }

  /** Regression test for b/147344912. */
  @Test
  public void varargsSeparateCompilation() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import " + Holder.class.getCanonicalName() + ";",
            "class Test {",
            "  void bar() {",
            "    Holder.varargsMethod(/* values...= */ 1, 1, 1);",
            "  }",
            "}")
        .withClasspath(Holder.class, ParameterNameTest.class)
        .doTest();
  }

  @Test
  public void exemptPackage() {
    CompilationTestHelper.newInstance(ParameterName.class, getClass())
        .addSourceLines(
            "test/a/A.java",
            "package test.a;",
            "public class A {",
            "  public static void f(int value) {}",
            "}")
        .addSourceLines(
            "test/b/nested/B.java",
            "package test.b.nested;",
            "public class B {",
            "  public static void f(int value) {}",
            "}")
        .addSourceLines(
            "test/c/C.java",
            "package test.c;",
            "public class C {",
            "  public static void f(int value) {}",
            "}")
        .addSourceLines(
            "Test.java",
            "import test.a.A;",
            "import test.b.nested.B;",
            "import test.c.C;",
            "class Test {",
            "  void f() {",
            "    A.f(/* typo= */ 1);",
            "    B.f(/* typo= */ 1);",
            "    // BUG: Diagnostic contains: 'C.f(/* value= */ 1);'",
            "    C.f(/* typo= */ 1);",
            "  }",
            "}")
        .setArgs(ImmutableList.of("-XepOpt:ParameterName:exemptPackagePrefixes=test.a,test.b"))
        .doTest();
  }
}
