/*
 * Copyright 2018 The Error Prone Authors.
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
import static org.junit.Assume.assumeTrue;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link AnnotationPosition} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class AnnotationPositionTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(AnnotationPosition.class, getClass())
          .addInputLines(
              "TypeUse.java",
              "import java.lang.annotation.ElementType;",
              "import java.lang.annotation.Target;",
              "@Target({ElementType.TYPE_USE})",
              "@interface TypeUse {",
              "  String value() default \"\";",
              "}")
          .expectUnchanged()
          .addInputLines(
              "EitherUse.java",
              "import java.lang.annotation.ElementType;",
              "import java.lang.annotation.Target;",
              "@Target({ElementType.TYPE_USE, ElementType.METHOD, ElementType.TYPE})",
              "@interface EitherUse {",
              "  String value() default \"\";",
              "}")
          .expectUnchanged()
          .addInputLines(
              "NonTypeUse.java", //
              "@interface NonTypeUse {}")
          .expectUnchanged();

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(AnnotationPosition.class, getClass())
          .addSourceLines(
              "TypeUse.java",
              "import java.lang.annotation.ElementType;",
              "import java.lang.annotation.Target;",
              "@Target({ElementType.TYPE_USE, ElementType.METHOD, ElementType.TYPE})",
              "@interface TypeUse {",
              "  String value() default \"\";",
              "}")
          .addSourceLines(
              "NonTypeUse.java", //
              "@interface NonTypeUse {}")
          .addSourceLines(
              "EitherUse.java",
              "import java.lang.annotation.ElementType;",
              "import java.lang.annotation.Target;",
              "@Target({ElementType.TYPE_USE, ElementType.METHOD, ElementType.TYPE})",
              "@interface EitherUse {",
              "  String value() default \"\";",
              "}");

  @Test
  public void nonTypeAnnotation() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "interface Test {", //
            "  public @Override boolean equals(Object o);",
            "}")
        .addOutputLines(
            "Test.java", //
            "interface Test {",
            "  @Override public boolean equals(Object o);",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void interspersedJavadoc() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  @NonTypeUse",
            "  /** Javadoc! */",
            "  public void foo();",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** Javadoc! */",
            "  @NonTypeUse",
            "  public void foo();",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void interspersedJavadoc_treeAlreadyHasJavadoc_noSuggestion() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /** Actually Javadoc. */",
            "  @NonTypeUse",
            "  /** Javadoc! */",
            "  public void foo();",
            "}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void interspersedJavadoc_withComment() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  @NonTypeUse",
            "  /** Javadoc! */",
            "  // TODO: fix",
            "  public void foo();",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** Javadoc! */",
            "  @NonTypeUse",
            "  // TODO: fix",
            "  public void foo();",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negatives() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /** Javadoc */",
            "  @NonTypeUse",
            "  public boolean foo();",
            "  @NonTypeUse",
            "  public boolean bar();",
            "  public @EitherUse boolean baz();",
            "  /** Javadoc */",
            "  @NonTypeUse",
            "  // comment",
            "  public boolean quux();",
            "}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative_parameter() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  public boolean foo(final @NonTypeUse String s);",
            "}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void typeAnnotation() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /** Javadoc */",
            "  public @NonTypeUse @EitherUse String foo();",
            "  /** Javadoc */",
            "  public @EitherUse @NonTypeUse String bar();",
            "  public @EitherUse /** Javadoc */ @NonTypeUse String baz();",
            "  public @EitherUse static @NonTypeUse int quux() { return 1; }",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** Javadoc */",
            "  @NonTypeUse public @EitherUse String foo();",
            "  /** Javadoc */",
            "  @NonTypeUse public @EitherUse String bar();",
            "  /** Javadoc */",
            "  @NonTypeUse public @EitherUse String baz();",
            "  @NonTypeUse public static @EitherUse int quux() { return 1; }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void variables() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  public @EitherUse static /** Javadoc */ @NonTypeUse int foo = 1;",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** Javadoc */",
            "  @NonTypeUse public static @EitherUse int foo = 1;",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void classes() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "public @NonTypeUse",
            "interface Test {}")
        .addOutputLines(
            "Test.java", //
            "@NonTypeUse",
            "public interface Test {}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void class_typeUseBeforeModifiers() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "public @EitherUse interface Test {}")
        .addOutputLines(
            "Test.java", //
            "@EitherUse",
            "public interface Test {}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void class_intermingledJavadoc() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "@NonTypeUse public /** Javadoc */ final class Test {}")
        .addOutputLines(
            "Test.java", //
            "/** Javadoc */",
            "@NonTypeUse public final class Test {}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void betweenModifiers() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  public @EitherUse static @NonTypeUse int foo() { return 1; }",
            "  public @EitherUse @NonTypeUse static int bar() { return 1; }",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  @NonTypeUse public static @EitherUse int foo() { return 1; }",
            "  @NonTypeUse public static @EitherUse int bar() { return 1; }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void betweenModifiersWithValue() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  public final @EitherUse(\"foo\") int foo(final int a) { return 1; }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  public final @EitherUse(\"foo\") int foo(final int a) { return 1; }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void interspersedComments() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  public @EitherUse /** Javadoc */ @NonTypeUse String baz();",
            "  /* a */ public /* b */ @EitherUse /* c */ static /* d */ "
                + "@NonTypeUse /* e */ int quux() { return 1; }",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** Javadoc */",
            "  @NonTypeUse public @EitherUse String baz();",
            "  /* a */ @NonTypeUse public /* b */ /* c */ static @EitherUse "
                + "/* d */ /* e */ int quux() { return 1; }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void messages() {
    helper
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  // BUG: Diagnostic contains: @Override is not a TYPE_USE annotation",
            "  public @Override boolean equals(Object o);",
            "  // BUG: Diagnostic contains: @Override, @NonTypeUse are not TYPE_USE annotations",
            "  public @Override @NonTypeUse int hashCode();",
            "  // BUG: Diagnostic contains: Javadocs should appear before any modifiers",
            "  @NonTypeUse /** Javadoc */ public boolean bar();",
            "}")
        .doTest();
  }

  @Test
  public void diagnostic() {
    helper
        .addSourceLines(
            "Test.java", //
            "interface Test {",
            "  // BUG: Diagnostic contains: is a TYPE_USE",
            "  public @EitherUse static int foo = 1;",
            "}")
        .doTest();
  }

  // TODO(b/168625474): 'sealed' doesn't have a TokenKind
  @Test
  public void sealedInterface() {
    assumeTrue(RuntimeVersion.isAtLeast15());
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "/** Javadoc! */",
            "sealed @Deprecated interface Test {",
            "  final class A implements Test {}",
            "}")
        .addOutputLines(
            "Test.java", //
            "/** Javadoc! */",
            "sealed @Deprecated interface Test {",
            "  final class A implements Test {}",
            "}")
        .setArgs("--enable-preview", "--release", Integer.toString(RuntimeVersion.release()))
        .doTest(TEXT_MATCH);
  }

  @Test
  public void typeArgument() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "interface T {",
            "  @EitherUse <T> T f();",
            "}")
        .addOutputLines(
            "Test.java", //
            "interface T {",
            "  <T> @EitherUse T f();",
            "}")
        .doTest();
  }

  @Test
  public void typeUseAndNonTypeUse_inWrongOrder() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "interface T {",
            "  @TypeUse @NonTypeUse T f();",
            "}")
        .addOutputLines(
            "Test.java", //
            "interface T {",
            "  @NonTypeUse @TypeUse T f();",
            "}")
        .doTest();
  }

  @Test
  public void annotationOfEitherUse_isAllowedToRemainBeforeModifiers() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "interface T {",
            "  @NonTypeUse @EitherUse public T a();",
            "  @NonTypeUse public @EitherUse T b();",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
