/*
 * Copyright 2018 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link UnusedMethod}. */
@RunWith(JUnit4.class)
public final class UnusedMethodTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnusedMethod.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(UnusedMethod.class, getClass());

  @Test
  public void unusedNative() {
    helper
        .addSourceLines(
            "UnusedNative.java",
            "package unusedvars;",
            "public class UnusedNative {",
            "  private native void aNativeMethod();",
            "}")
        .doTest();
  }

  @Test
  public void unusedPrivateMethod() {
    helper
        .addSourceLines(
            "UnusedPrivateMethod.java",
            "package unusedvars;",
            "import com.google.errorprone.annotations.Keep;",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "import java.lang.annotation.Target;",
            "import javax.inject.Inject;",
            "public class UnusedPrivateMethod {",
            "  public void test() {",
            "    used();",
            "  }",
            "  private void used() {}",
            "  // BUG: Diagnostic contains: Method 'notUsed' is never used.",
            "  private void notUsed() {}",
            "  @Inject",
            "  private void notUsedExempted() {}",
            "  @Keep",
            "  @Target(ElementType.METHOD)",
            "  @Retention(RetentionPolicy.SOURCE)",
            "  private @interface ProvidesCustom {}",
            "}")
        .doTest();
  }

  @Test
  public void unuseds() {
    helper
        .addSourceLines(
            "Unuseds.java",
            "package unusedvars;",
            "import java.io.IOException;",
            "import java.io.ObjectStreamException;",
            "import java.util.List;",
            "import javax.inject.Inject;",
            "public class Unuseds {",
            "  // BUG: Diagnostic contains:",
            "  private void notUsedMethod() {}",
            "  // BUG: Diagnostic contains:",
            "  private static void staticNotUsedMethod() {}",
            "  @SuppressWarnings({\"deprecation\", \"unused\"})",
            "  class UsesSuppressWarning {",
            "    private int f1;",
            "    private void test1() {",
            "      int local;",
            "    }",
            "    @SuppressWarnings(value = \"unused\")",
            "    private void test2() {",
            "      int local;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void exemptedMethods() {
    helper
        .addSourceLines(
            "Unuseds.java",
            "package unusedvars;",
            "import java.io.IOException;",
            "import java.io.ObjectStreamException;",
            "public class Unuseds implements java.io.Serializable {",
            "  private void readObject(java.io.ObjectInputStream in) throws IOException {",
            "    in.hashCode();",
            "  }",
            "  private void writeObject(java.io.ObjectOutputStream out) throws IOException {",
            "    out.writeInt(123);",
            "  }",
            "  private Object readResolve() {",
            "    return null;",
            "  }",
            "  private void readObjectNoData() throws ObjectStreamException {}",
            "}")
        .doTest();
  }

  @Test
  public void exemptedByName() {
    helper
        .addSourceLines(
            "Unuseds.java",
            "package unusedvars;",
            "class ExemptedByName {",
            "  private void unused1(int a, int unusedParam) {",
            "    int unusedLocal = a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressions() {
    helper
        .addSourceLines(
            "Unuseds.java",
            "package unusedvars;",
            "class Suppressed {",
            "  @SuppressWarnings({\"deprecation\", \"unused\"})",
            "  class UsesSuppressWarning {",
            "    private void test1() {",
            "      int local;",
            "    }",
            "    @SuppressWarnings(value = \"unused\")",
            "    private void test2() {",
            "      int local;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void removal_javadocsAndNonJavadocs() {
    refactoringHelper
        .addInputLines(
            "UnusedWithComment.java",
            "package unusedvars;",
            "public class UnusedWithComment {",
            "  /**",
            "   * Method comment",
            "   */private void test1() {",
            "  }",
            "  /** Method comment */",
            "  private void test2() {",
            "  }",
            "  // Non javadoc comment",
            "  private void test3() {",
            "  }",
            "}")
        .addOutputLines(
            "UnusedWithComment.java", //
            "package unusedvars;",
            "public class UnusedWithComment {",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void usedInLambda() {
    helper
        .addSourceLines(
            "UsedInLambda.java",
            "package unusedvars;",
            "import java.util.Arrays;",
            "import java.util.List;",
            "import java.util.function.Function;",
            "import java.util.stream.Collectors;",
            "/** Method parameters used in lambdas and anonymous classes */",
            "public class UsedInLambda {",
            "  private Function<Integer, Integer> usedInLambda() {",
            "    return x -> 1;",
            "  }",
            "  private String print(Object o) {",
            "    return o.toString();",
            "  }",
            "  public List<String> print(List<Object> os) {",
            "    return os.stream().map(this::print).collect(Collectors.toList());",
            "  }",
            "  public static void main(String[] args) {",
            "    System.err.println(new UsedInLambda().usedInLambda());",
            "    System.err.println(new UsedInLambda().print(Arrays.asList(1, 2, 3)));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void onlyForMethodReference() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Predicate;",
            "class Test {",
            "  private static boolean foo(int a) {",
            "    return true;",
            "  }",
            "  Predicate<Integer> pred = Test::foo;",
            "}")
        .doTest();
  }

  @Test
  public void methodSource() {
    helper
        .addSourceLines(
            "MethodSource.java",
            "package org.junit.jupiter.params.provider;",
            "public @interface MethodSource {",
            "  String[] value();",
            "}")
        .addSourceLines(
            "Test.java",
            "import java.util.stream.Stream;",
            "import org.junit.jupiter.params.provider.MethodSource;",
            "class Test {",
            "  @MethodSource(\"parameters\")",
            "  void test() {}",
            "",
            "  private static Stream<String> parameters() {",
            "    return Stream.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifiedMethodSource() {
    helper
        .addSourceLines(
            "MethodSource.java",
            "package org.junit.jupiter.params.provider;",
            "public @interface MethodSource {",
            "  String[] value();",
            "}")
        .addSourceLines(
            "Test.java",
            "import java.util.stream.Stream;",
            "import org.junit.jupiter.params.provider.MethodSource;",
            "class Test {",
            "  @MethodSource(\"Test#parameters\")",
            "  void test() {}",
            "",
            "",
            "  private static Stream<String> parameters() {",
            "    return Stream.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nestedQualifiedMethodSource() {
    helper
        .addSourceLines(
            "MethodSource.java",
            "package org.junit.jupiter.params.provider;",
            "public @interface MethodSource {",
            "  String[] value();",
            "}")
        .addSourceLines(
            "Test.java",
            "import java.util.stream.Stream;",
            "import org.junit.jupiter.params.provider.MethodSource;",
            "class Test {",
            "  // @Nested",
            "  public class NestedTest {",
            "    @MethodSource(\"Test#parameters\")",
            "    void test() {}",
            "  }",
            "",
            "  private static Stream<String> parameters() {",
            "    return Stream.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void overriddenMethodNotCalledWithinClass() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private class Inner {",
            "    @Override public String toString() { return null; }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodWithinPrivateInnerClass_isEligible() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private class Inner {",
            "    // BUG: Diagnostic contains:",
            "    public void foo() {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedConstructor() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  // BUG: Diagnostic contains: Constructor 'Test'",
            "  private Test(int a) {}",
            "}")
        .doTest();
  }

  @Test
  public void unusedConstructor_refactoredToPrivateNoArgVersion() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  private Test(int a) {}",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  private Test() {}",
            "}")
        .doTest();
  }

  @Test
  public void unusedConstructor_finalFieldsLeftDangling_noFix() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  private final int a;",
            "  private Test(int a) {",
            "    this.a = a;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void unusedConstructor_nonFinalFields_stillRefactored() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  private int a;",
            "  private Test(int a) {}",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  private int a;",
            "  private Test() {}",
            "}")
        .doTest();
  }

  @Test
  public void unusedConstructor_removed() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  private Test(int a) {}",
            "  private Test(String a) {}",
            "  public Test of() { return new Test(1); }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  private Test(int a) {}",
            "  public Test of() { return new Test(1); }",
            "}")
        .doTest();
  }

  @Test
  public void privateConstructor_calledWithinClass() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  private Test(int a) {}",
            "  public Test of(int a) {",
            "    return new Test(a);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void zeroArgConstructor_notFlagged() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  private Test() {}",
            "}")
        .doTest();
  }

  @Test
  public void annotationProperty_assignedByname() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private @interface Anno {",
            "    int value() default 1;",
            "  }",
            "  @Anno(value = 1) int b;",
            "}")
        .doTest();
  }

  @Test
  public void annotationProperty_assignedAsDefault() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private @interface Anno {",
            "    int value();",
            "  }",
            "  @Anno(1) int a;",
            "}")
        .doTest();
  }
}
