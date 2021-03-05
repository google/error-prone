/*
 * Copyright 2019 The Error Prone Authors.
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

/** Tests for {@link FieldCanBeLocal}. */
@RunWith(JUnit4.class)
public final class FieldCanBeLocalTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(FieldCanBeLocal.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(FieldCanBeLocal.class, getClass());

  @Test
  public void simplePositive() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  private int a;",
            "  int foo() {",
            "    a = 1;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void multipleAssignments() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  int foo() {",
            "    a = 1;",
            "    a = 2;",
            "    return a;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  int foo() {",
            "    int a = 1;",
            "    a = 2;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hasFieldAnnotation_noMatch() {
    helper
        .addSourceLines(
            "Field.java",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "import java.lang.annotation.Target;",
            "@Target(ElementType.FIELD)",
            "@Retention(RetentionPolicy.RUNTIME)",
            "@interface Field {}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @Field private int a;",
            "  int foo() {",
            "    a = 1;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hasVariableAnnotation_matchesAndAnnotationCopied() {
    refactoringTestHelper
        .addInputLines(
            "Field.java",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "import java.lang.annotation.Target;",
            "@Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE})",
            "@Retention(RetentionPolicy.RUNTIME)",
            "@interface Field {}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "class Test {",
            "  @Field private int a;",
            "  int foo() {",
            "    a = 1;",
            "    return a;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  int foo() {",
            "    @Field int a = 1;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void multipleVariableAnnotations() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import javax.annotation.Nonnull;",
            "import javax.annotation.Nullable;",
            "class Test {",
            "  @Nonnull /* foo */ @Nullable private Integer a;",
            "  int foo() {",
            "    a = 1;",
            "    return a;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import javax.annotation.Nonnull;",
            "import javax.annotation.Nullable;",
            "class Test {",
            "",
            "  int foo() {",
            "    @Nonnull /* foo */ @Nullable Integer a = 1;",
            "    return a;",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void hasTypeUseAnnotation_match() {
    refactoringTestHelper
        .addInputLines(
            "Field.java",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "import java.lang.annotation.Target;",
            "@Target(ElementType.TYPE_USE)",
            "@Retention(RetentionPolicy.RUNTIME)",
            "@interface Field {}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "class Test {",
            "  @Field private int a;",
            "  int foo() {",
            "    a = 1;",
            "    return a;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  int foo() {",
            "    @Field int a = 1;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressedOnMethod() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @SuppressWarnings(\"FieldCanBeLocal\")",
            "  private int a;",
            "  int foo() {",
            "    a = 1;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressedOnClass() {
    helper
        .addSourceLines(
            "Test.java",
            "@SuppressWarnings(\"FieldCanBeLocal\")",
            "class Test {",
            "  private int a;",
            "  int foo() {",
            "    a = 1;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inlineConditional_noWarning() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  int foo(int b) {",
            "    a = b > 2 ? a : b;",
            "    return a;",
            "  }",
            "  int bar(int b) {",
            "    a = b;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fieldIsPublic_noMatch() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public int a;",
            "  int foo() {",
            "    a = 1;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void usedBeforeAssigment_noMatch() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public int a;",
            "  int foo() {",
            "    if (a < 0) {",
            "      return 0;",
            "    }",
            "    a = 1;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void usedInMultipleMethods_alwaysAssignedFirst_positive() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  int foo() {",
            "    a = 1;",
            "    return a;",
            "  }",
            "  int bar() {",
            "    a = 2;",
            "    return a;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  int foo() {",
            "    int a = 1;",
            "    return a;",
            "  }",
            "  int bar() {",
            "    int a = 2;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifiedWithThis_refactoringRemovesThis() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  int foo() {",
            "    this.a = 1;",
            "    return a;",
            "  }",
            "  int bar() {",
            "    this.a = 2;",
            "    return a;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  int foo() {",
            "    int a = 1;",
            "    return a;",
            "  }",
            "  int bar() {",
            "    int a = 2;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assignmentToFieldOfSameName_isRemoved() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  Test(int a) {",
            "    this.a = a;",
            "    int b = a + 2;",
            "  }",
            "  int foo() {",
            "    this.a = 1;",
            "    return a;",
            "  }",
            "  int bar() {",
            "    this.a = 2;",
            "    return a;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  Test(int a) {",
            "    int b = a + 2;",
            "  }",
            "  int foo() {",
            "    int a = 1;",
            "    return a;",
            "  }",
            "  int bar() {",
            "    int a = 2;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void usedBeforeReassignment_noMatch() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  int foo() {",
            "    a = a + 1;",
            "    return a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fieldAssignedOnField() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  class Sub {",
            "    private int a;",
            "    int a() {",
            "      return a;",
            "    }",
            "  }",
            "  private Sub sub;",
            "  Test(Sub sub) {",
            "    this.sub = sub;",
            "  }",
            "  void foo() {",
            "    sub.a = 1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void usedWithinClassScope_noMatch() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Predicate;",
            "class Test {",
            "  private Integer a;",
            "  Predicate<Integer> predicate = b -> a == b;",
            "  Test(int a) {",
            "    this.a = a;",
            "  }",
            "  public void set(int a) {",
            "    this.a = a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void usedWithinLambda_noWarning() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Predicate;",
            "class Test {",
            "  private Integer a;",
            "  Test(int a) {",
            "    this.a = a;",
            "  }",
            "  public Predicate<Integer> set(int a) {",
            "    this.a = a;",
            "    return x -> x == this.a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void usedWithinLambdaMemberSelect() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Predicate;",
            "import java.util.stream.Stream;",
            "import java.util.Collections;",
            "class Test {",
            "  private Integer a;",
            "  Test(int a) {",
            "    this.a = a;",
            "  }",
            "  public Stream<Integer> set(int a) {",
            "    this.a = a;",
            "    return Collections.<Integer>emptyList().stream()",
            "        .filter(x -> x == this.a)",
            "        .filter(x -> x > 0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void usedInStaticInitializer() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static {",
            "    Test[] tests = new Test[0];",
            "    for (Test test : tests) {",
            "      int b = test.a;",
            "    }",
            "  }",
            "  private Integer a;",
            "  Test(int a) {",
            "    this.a = a;",
            "  }",
            "}")
        .doTest();
  }
}
