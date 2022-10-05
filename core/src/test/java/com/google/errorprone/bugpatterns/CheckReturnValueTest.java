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

import com.google.common.base.Joiner;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
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

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(CheckReturnValue.class, getClass());

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
            "    // BUG: Diagnostic contains: CheckReturnValue",
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
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
            "  // BUG: Diagnostic contains: CheckReturnValue",
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
            "  // BUG: Diagnostic contains: CheckReturnValue",
            "  // @CheckReturnValue may not be applied to void-returning methods",
            "  @com.google.errorprone.annotations.CheckReturnValue public static Void f() {",
            "    return null;",
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
            "  // BUG: Diagnostic contains: CheckReturnValue",
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    x.get(0);",
            "  }",
            "}")
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    list.get(0);",
            "    // BUG: Diagnostic contains: CheckReturnValue",
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
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
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    list.get(0);",
            "    pattern.matcher(\"blah\");",
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    new PatternSyntaxException(\"\", \"\", 0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringAssignsToOriginalBasedOnSubstitutedTypes() {
    refactoringHelper
        .addInputLines(
            "Builder.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "interface Builder<B extends Builder<B>> {",
            "  B setFoo(String s);",
            "}")
        .expectUnchanged()
        .addInputLines(
            "SomeBuilder.java", //
            "interface SomeBuilder extends Builder<SomeBuilder> {}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f(SomeBuilder builder, String s) {",
            "    builder.setFoo(s);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void f(SomeBuilder builder, String s) {",
            "    builder = builder.setFoo(s);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSuggestCanIgnoreReturnValueForMethodInvocation() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CheckReturnValue",
            "class Test {",
            "  void foo() {",
            "    makeBarOrThrow();",
            "  }",
            "  String makeBarOrThrow() {",
            "    throw new UnsupportedOperationException();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CheckReturnValue",
            "class Test {",
            "  void foo() {",
            "    makeBarOrThrow();",
            "  }",
            "  @CanIgnoreReturnValue",
            "  String makeBarOrThrow() {",
            "    throw new UnsupportedOperationException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSuggestCanIgnoreReturnValueForMethodReference() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CheckReturnValue",
            "class Test {",
            "  Runnable r = this::makeBarOrThrow;",
            "  String makeBarOrThrow() {",
            "    throw new UnsupportedOperationException();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CheckReturnValue",
            "class Test {",
            "  Runnable r = this::makeBarOrThrow;",
            "  @CanIgnoreReturnValue",
            "  String makeBarOrThrow() {",
            "    throw new UnsupportedOperationException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSuggestCanIgnoreReturnValueForConstructor() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CheckReturnValue",
            "class Test {",
            "  Test() {}",
            "  void run() {",
            "    new Test();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CheckReturnValue",
            "class Test {",
            "  @CanIgnoreReturnValue",
            "  Test() {}",
            "  void run() {",
            "    new Test();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSuggestCanIgnoreReturnValueAndRemoveCheckReturnValue() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "class Test {",
            "  void foo() {",
            "    makeBarOrThrow();",
            "  }",
            "  @CheckReturnValue",
            "  String makeBarOrThrow() {",
            "    throw new UnsupportedOperationException();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "class Test {",
            "  void foo() {",
            "    makeBarOrThrow();",
            "  }",
            "  @CanIgnoreReturnValue",
            "  String makeBarOrThrow() {",
            "    throw new UnsupportedOperationException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDoesNotSuggestCanIgnoreReturnValueForOtherFile() {
    refactoringHelper
        .addInputLines(
            "Lib.java",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CheckReturnValue",
            "class Lib {",
            "  String makeBarOrThrow() {",
            "    throw new UnsupportedOperationException();",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CheckReturnValue",
            "class Test {",
            "  void foo(Lib l) {",
            "    l.makeBarOrThrow();",
            "  }",
            "}")
        // The checker doesn't suggest CIRV, so it applies a different fix instead.
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CheckReturnValue",
            "class Test {",
            "  void foo(Lib l) {",
            "    var unused = l.makeBarOrThrow();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSuggestsVarUnusedForConstructor() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "class Test {",
            "  void go() {",
            "    new Test();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "@com.google.errorprone.annotations.CheckReturnValue",
            "class Test {",
            "  void go() {",
            "    var unused = new Test();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSuggestsVarUnused2() {
    refactoringHelper
        .addInputLines(
            "Lib.java",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CheckReturnValue",
            "interface Lib {",
            "  int a();",
            "  int b();",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void foo(Lib lib) {",
            "    var unused = lib.a();",
            "    lib.b();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void foo(Lib lib) {",
            "    var unused = lib.a();",
            "    var unused2 = lib.b();",
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
