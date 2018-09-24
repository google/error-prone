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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
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
  private static final String[] typeUseLines =
      new String[] {
        "import java.lang.annotation.ElementType;",
        "import java.lang.annotation.Target;",
        "@Target({ElementType.TYPE_USE, ElementType.METHOD, ElementType.TYPE})",
        "@interface TypeUse {}"
      };

  private static final String[] nonTypeUseLines = new String[] {"@interface NonTypeUse {}"};

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new AnnotationPosition(), getClass())
          .addInputLines("TypeUse.java", typeUseLines)
          .expectUnchanged()
          .addInputLines("NonTypeUse.java", nonTypeUseLines)
          .expectUnchanged();

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(AnnotationPosition.class, getClass())
          .addSourceLines("TypeUse.java", typeUseLines)
          .addSourceLines("NonTypeUse.java", nonTypeUseLines);

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
        .doTest(TestMode.TEXT_MATCH);
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
        .doTest(TestMode.TEXT_MATCH);
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
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void negatives() {
    helper
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  /** Javadoc */",
            "  @NonTypeUse",
            "  public boolean foo();",
            "  @NonTypeUse",
            "  public boolean bar();",
            "  public @TypeUse boolean baz();",
            "  /** Javadoc */",
            "  @NonTypeUse",
            "  // comment",
            "  public boolean quux();",
            "}")
        .doTest();
  }

  @Test
  public void negative_parameter() {
    helper
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  public boolean foo(final @NonTypeUse String s);",
            "}")
        .doTest();
  }

  @Test
  public void typeAnnotation() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  /** Javadoc */",
            "  public @NonTypeUse @TypeUse String foo();",
            "  /** Javadoc */",
            "  public @TypeUse @NonTypeUse String bar();",
            "  public @TypeUse /** Javadoc */ @NonTypeUse String baz();",
            "  public @TypeUse static @NonTypeUse int quux() { return 1; }",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** Javadoc */",
            "  @NonTypeUse public @TypeUse String foo();",
            "  /** Javadoc */",
            "  @NonTypeUse public @TypeUse String bar();",
            "  /** Javadoc */",
            "  @NonTypeUse public @TypeUse String baz();",
            "  @NonTypeUse public static @TypeUse int quux() { return 1; }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void variables() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  public @TypeUse static /** Javadoc */ @NonTypeUse int foo = 1;",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** Javadoc */",
            "  @NonTypeUse public static @TypeUse int foo = 1;",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void classes() {
    refactoringHelper
        .addInputLines("Test.java", "public @NonTypeUse", "interface Test {}")
        .addOutputLines("Test.java", "@NonTypeUse", "public interface Test {}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void class_typeUseBeforeModifiers() {
    refactoringHelper
        .addInputLines("Test.java", "public @TypeUse interface Test {}")
        .addOutputLines("Test.java", "@TypeUse", "public interface Test {}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void class_intermingledJavadoc() {
    refactoringHelper
        .addInputLines("Test.java", "@NonTypeUse public /** Javadoc */ final class Test {}")
        .addOutputLines("Test.java", "/** Javadoc */", "@NonTypeUse public final class Test {}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void betweenModifiers() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  public @TypeUse static @NonTypeUse int foo() { return 1; }",
            "  public @TypeUse @NonTypeUse static int bar() { return 1; }",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  @NonTypeUse public static @TypeUse int foo() { return 1; }",
            "  @NonTypeUse public static @TypeUse int bar() { return 1; }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void interspersedComments() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "interface Test {",
            "  public @TypeUse /** Javadoc */ @NonTypeUse String baz();",
            "  /* a */ public /* b */ @TypeUse /* c */ static /* d */ "
                + "@NonTypeUse /* e */ int quux() { return 1; }",
            "}")
        .addOutputLines(
            "Test.java",
            "interface Test {",
            "  /** Javadoc */",
            "  @NonTypeUse public @TypeUse String baz();",
            "  /* a */ @NonTypeUse public /* b */ /* c */ static @TypeUse "
                + "/* d */ /* e */ int quux() { return 1; }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void messages() {
    helper
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  // BUG: Diagnostic contains: @Override is not a type annotation",
            "  public @Override boolean equals(Object o);",
            "  // BUG: Diagnostic contains: @Override, @NonTypeUse are not type annotations",
            "  public @Override @NonTypeUse int hashCode();",
            "  // BUG: Diagnostic contains: Javadocs should appear before any modifiers",
            "  @NonTypeUse /** Javadoc */ public boolean bar();",
            "}")
        .doTest();
  }
}
