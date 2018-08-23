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
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f(int foo, int bar) {}",
            "  {",
            "    f(/* foo= */ 1, /* bar= */ 2);",
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
}
