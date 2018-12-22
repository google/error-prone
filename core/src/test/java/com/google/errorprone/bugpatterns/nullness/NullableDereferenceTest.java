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

package com.google.errorprone.bugpatterns.nullness;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author bennostein@google.com (Benno Stein) */
@RunWith(JUnit4.class)
public class NullableDereferenceTest {

  @Test
  public void testAnnotatedFormal() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/AnnotatedFormalTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class AnnotatedFormalTest {",
            "  public void testPos(@Nullable Object nullable) {",
            "    // BUG: Diagnostic contains: possibly null",
            "    nullable.toString();",
            "  }",
            "  public void testNeg(Object unannotated, @NonNull Object nonnull) {",
            "    unannotated.toString();",
            "    nonnull.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAnnotatedField() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/AnnotatedFieldTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class AnnotatedFieldTest<T extends @Nullable Object> {",
            "  Object f1;",
            "  @Nullable Object f2;",
            "  @NonNull Object f3;",
            "  T f4;",
            "  @Nullable T f5;",
            "  @NonNull T f6;",
            "  public void test() {",
            "    f1.toString();",
            "    // BUG: Diagnostic contains: possibly null",
            "    f2.toString();",
            "    f3.toString();",
            "    // BUG: Diagnostic contains: possibly null",
            "    f4.toString();",
            "    // BUG: Diagnostic contains: possibly null",
            "    f5.toString();",
            "    f6.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDataflow() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/DataflowTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class DataflowTest {",
            "  @Nullable Object f;",
            "  public void testFormal(@Nullable Object o) {",
            "    // BUG: Diagnostic contains: possibly null",
            "    o.toString();",
            // no error here: successful deref on previous line implies `o` is not null
            "    o.toString();",
            "  }",
            "  public void testFieldDeref() {",
            "    // BUG: Diagnostic contains: possibly null",
            "    f.toString();",
            // no error here: successful deref on previous line implies `o` is not null
            "    f.toString();",
            "  }",
            "  public void testFieldAssignment() {",
            "    f = \"hello\";",
            "    f.toString();",
            "    f = null;",
            "    // BUG: Diagnostic contains: definitely null",
            "    f.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDataflow_unannotated() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/DataflowTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class DataflowTest {",
            "  public void test(Object o) {",
            "    o.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMethodParameters() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/DataflowTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class DataflowTest {",
            "  public void test1() {",
            "    // BUG: Diagnostic contains: definitely null",
            "    callee1(null);",
            "    callee1(this);",
            "    callee2(null);",
            "    callee3(null);",
            "  }",
            "  public void test2(@Nullable Object o) {",
            "    // BUG: Diagnostic contains: possibly null",
            "    callee1(o);",
            "    callee2(o);",
            "    callee3(o);",
            "  }",
            "  public void test3(@NonNull Object o) {",
            "    callee1(o);",
            "    callee2(o);",
            "    callee3(o);",
            "  }",
            "  public void test4(Object o) {",
            "    callee1(o);",
            "    callee2(o);",
            "    callee3(o);",
            "  }",
            "  public void callee1(@NonNull Object o) {",
            "  }",
            "  public void callee2(@Nullable Object o) {",
            "  }",
            "  public void callee3(Object o) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDataflowAfterMethodResult() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/DataflowTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class DataflowTest {",
            "  public void test(@Nullable Object o) {",
            "    callee1().toString();",
            "    // BUG: Diagnostic contains: possibly null",
            "    callee2().toString();",
            "    callee3().toString();",
            "  }",
            "  public @NonNull Object callee1() {",
            "    return this;",
            "  }",
            "  public @Nullable Object callee2() {",
            "    return null;",
            "  }",
            "  public Object callee3() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInference() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/InferenceTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class InferenceTest {",
            "  public void test(@Nullable Object nullable, @NonNull Object nonnull) {",
            "    // BUG: Diagnostic contains: possibly null",
            "    nullable.toString();",
            "    // BUG: Diagnostic contains: possibly null",
            "    id(nullable).toString();",
            "    nonnull.toString();",
            "    id(nonnull).toString();",
            "  }",
            "  static <T> T id(T t) { return t; }",
            "}")
        .doTest();
  }

  @Test
  public void testNewClass() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/DataflowTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class DataflowTest {",
            "  public void test1() {",
            "    // BUG: Diagnostic contains: definitely null",
            "    new Inner((String) null);",
            "    new Inner((Integer) null);",
            "    new Inner((Double) null);",
            "    new Inner(\"hello\");",
            "    new Inner(5);",
            "    new Inner(7d);",
            "  }",
            "  public void test2(@Nullable String s, @Nullable Integer i, @Nullable Double d) {",
            "    // BUG: Diagnostic contains: possibly null",
            "    new Inner(s);",
            "    new Inner(i);",
            "    new Inner(d);",
            "  }",
            "  public void test3(@NonNull String s, @NonNull Integer i, @NonNull Double d) {",
            "    new Inner(s);",
            "    new Inner(i);",
            "    new Inner(d);",
            "  }",
            "  public void test4(String s, Integer i, Double d) {",
            "    new Inner(s);",
            "    new Inner(i);",
            "    new Inner(d);",
            "  }",
            "  public void testOuter1() {",
            "    this.new Inner(1);",
            "    // BUG: Diagnostic contains: definitely null",
            "    ((DataflowTest) null).new Inner(2);",
            "    new DataflowTest().new Inner(3);",
            "  }",
            "  public void testOuter2(@Nullable DataflowTest o) {",
            "    // BUG: Diagnostic contains: possibly null",
            "    o.new Inner(1);",
            "  }",
            "  public void testOuter3(@NonNull DataflowTest o) {",
            "    o.new Inner(1);",
            "  }",
            "  public void testOuter4(DataflowTest o) {",
            "    o.new Inner(1);",
            "  }",
            "  class Inner {",
            "    Inner(@NonNull String s) {}",
            "    Inner(@Nullable Integer i) {}",
            "    Inner(Double d) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNullLiteral() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullLiteralTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NullLiteralTest {",
            "  public void test() {",
            "    // BUG: Diagnostic contains: definitely null",
            "    ((Object) null).toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testStatics() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/StaticsTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class StaticsTest {",
            "  static Object staticField;",
            "  Object instanceField;",
            "  public void test() {",
            "    System.out.println();",
            "    System.out.println(StaticsTest.staticField);",
            "    System.out.println(staticField);",
            "    System.out.println(this.instanceField);",
            "    System.out.println(instanceField);",
            "    // BUG: Diagnostic contains: definitely null",
            "    System.out.println(((StaticsTest) null).instanceField);",
            "  }",
            "}")
        .doTest();
  }

  // Regression test for https://github.com/google/error-prone/issues/1138
  @Test
  public void testNoCrashOnStaticImport() {
    createCompilationTestHelper()
        .addSourceLines(
            "Constraint.java",
            "package javax.validation;",
            "import static java.lang.annotation.ElementType.ANNOTATION_TYPE;",
            "import java.lang.annotation.Target;",
            "@Target({ ANNOTATION_TYPE })",
            "public @interface Constraint {}")
        .doTest();
  }

  private CompilationTestHelper createCompilationTestHelper() {
    return CompilationTestHelper.newInstance(NullableDereference.class, getClass());
  }
}
