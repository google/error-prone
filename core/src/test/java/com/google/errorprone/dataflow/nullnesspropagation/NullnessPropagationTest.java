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

package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.dataflow.DataFlow.expressionDataflow;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.common.base.Joiner;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author deminguyen@google.com (Demi Nguyen)
 */
@RunWith(JUnit4.class)
public class NullnessPropagationTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(NullnessPropagationChecker.class, getClass());

  /**
   * This method triggers the {@code BugPattern} used to test nullness propagation
   *
   * @param obj Variable whose nullness value is being checked
   * @return {@code obj} for use in expressions
   */
  public static Object triggerNullnessChecker(Object obj) {
    return obj;
  }

  /*
   * Methods that should never be called. These methods exist to force tests that pass a primitive
   * to decide whether they are testing (a) that primitive itself is null in which case they should
   * call triggerNullnessCheckerOnPrimitive or (b) that the result of autoboxing the primitive is
   * null, in which case it should call triggerNullnessCheckerOnBoxed. Of course, in either case,
   * the value should never be null. (The analysis isn't yet smart enough to detect this in all
   * cases.)
   *
   * Any call to these methods will produce a special error.
   */

  public static void triggerNullnessChecker(boolean b) {}

  public static void triggerNullnessChecker(byte b) {}

  public static void triggerNullnessChecker(char c) {}

  public static void triggerNullnessChecker(double d) {}

  public static void triggerNullnessChecker(float f) {}

  public static void triggerNullnessChecker(int i) {}

  public static void triggerNullnessChecker(long l) {}

  public static void triggerNullnessChecker(short s) {}

  /**
   * This method also triggers the {@code BugPattern} used to test nullness propagation, but it is
   * intended to be used only in the rare case of testing the result of boxing a primitive.
   */
  public static void triggerNullnessCheckerOnBoxed(Object obj) {}

  /*
   * These methods also trigger the {@code BugPattern} used to test nullness propagation, but they
   * are careful not to autobox their inputs.
   */

  public static void triggerNullnessCheckerOnPrimitive(boolean b) {}

  public static void triggerNullnessCheckerOnPrimitive(byte b) {}

  public static void triggerNullnessCheckerOnPrimitive(char c) {}

  public static void triggerNullnessCheckerOnPrimitive(double d) {}

  public static void triggerNullnessCheckerOnPrimitive(float f) {}

  public static void triggerNullnessCheckerOnPrimitive(int i) {}

  public static void triggerNullnessCheckerOnPrimitive(long l) {}

  public static void triggerNullnessCheckerOnPrimitive(short s) {}

  /** For {@link #testConstantsDefinedInOtherCompilationUnits}. */
  public static final String COMPILE_TIME_CONSTANT = "not null";
  /** For {@link #testConstantsDefinedInOtherCompilationUnits} as constant outside compilation. */
  @SuppressWarnings("GoodTime") // false positive
  public static final Integer NOT_COMPILE_TIME_CONSTANT = 421;

  @Test
  public void testTransferFunctions1() {
    compilationHelper.addSourceFile("NullnessPropagationTransferCases1.java").doTest();
  }

  @Test
  public void testTransferFunctions2() {
    compilationHelper.addSourceFile("NullnessPropagationTransferCases2.java").doTest();
  }

  @Test
  public void testTransferFunctions3() {
    compilationHelper.addSourceFile("NullnessPropagationTransferCases3.java").doTest();
  }

  @Test
  public void testTransferFunctions4() {
    compilationHelper.addSourceFile("NullnessPropagationTransferCases4.java").doTest();
  }

  @Test
  public void testTransferFunctions5() {
    compilationHelper.addSourceFile("NullnessPropagationTransferCases5.java").doTest();
  }

  @Test
  public void testTransferFunctions6() {
    compilationHelper.addSourceFile("NullnessPropagationTransferCases6.java").doTest();
  }

  @Test
  public void testTransferFunctions7() {
    compilationHelper.addSourceFile("NullnessPropagationTransferCases7.java").doTest();
  }

  @Test
  public void testTransferFunctions8() {
    compilationHelper.addSourceFile("NullnessPropagationTransferCases8.java").doTest();
  }

  @Test
  public void testThis() {
    compilationHelper
        .addSourceLines(
            "ThisNonNullTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "public class ThisNonNullTest {",
            "  public void instanceMethod() {",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(this);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testEquals() {
    compilationHelper
        .addSourceLines(
            "ThisEqualsTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "public class ThisEqualsTest {",
            "  @Override",
            "  public boolean equals(Object obj) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(obj);",
            "    return this == obj;",
            "  }",
            "  private void testEquals(Object arg) {",
            "   ThisEqualsTest thisEqualsTest = new ThisEqualsTest();",
            "   if (thisEqualsTest.equals(arg)) {",
            "     // BUG: Diagnostic contains: (Non-null)",
            "     triggerNullnessChecker(arg);",
            "   }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceof() {
    compilationHelper
        .addSourceLines(
            "InstanceofTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "public class InstanceofTest {",
            "  public static void m(Object o) {",
            "    if (o instanceof InstanceofTest) {",
            "      // BUG: Diagnostic contains: (Non-null)",
            "      triggerNullnessChecker(o);",
            "    } else {",
            "      // BUG: Diagnostic contains: (Nullable)",
            "      triggerNullnessChecker(o);",
            "    }",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(o);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testProtoGetters() {
    compilationHelper
        .addSourceLines(
            "InstanceofTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "public class InstanceofTest {",
            "  public static void m(TestProtoMessage o) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(o);",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(o.getMessage());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testArrayAccess() {
    compilationHelper
        .addSourceLines(
            "ArrayAccessTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "public class ArrayAccessTest {",
            "  public static void read(Integer[] a) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(a);",
            "    Integer result = a[0];",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(a);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(result);",
            "  }",
            "  public static void read(int[][] matrix) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(matrix);",
            "    int result = matrix[0][0];",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(matrix);",
            "  }",
            "  public static void write(int[] vector) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(vector);",
            "    vector[7] = 42;",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(vector);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFieldAccess() {
    compilationHelper
        .addSourceLines(
            "FieldAccessTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "public class FieldAccessTest {",
            "  public static void dereference(Coinductive o) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(o);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(o.f);",
            "    o.f = (Coinductive) new Object();",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(o.f);",
            "  }",
            "  abstract class Coinductive {",
            "    Coinductive f;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFieldReceivers() {
    compilationHelper
        .addSourceLines(
            "FieldReceiversTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "public class FieldReceiversTest {",
            "  Object f;",
            "  public FieldReceiversTest getSelf() { return this; }",
            "  public void test_different_receivers(FieldReceiversTest other) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(other);",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(this);",
            "    other.f = new Object();",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(other.f);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(this.f);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(f);",
            "    this.f = null;",
            "    // BUG: Diagnostic contains: (Null)",
            "    triggerNullnessChecker(this.f);",
            "    // BUG: Diagnostic contains: (Null)",
            "    triggerNullnessChecker(f);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFieldPathSensitivity() {
    compilationHelper
        .addSourceLines(
            "FieldPathSensitivityTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "public class FieldPathSensitivityTest {",
            "  public static void path_sensitivity(Coinductive o) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(o.f);",
            "    if (o.f == null) {",
            "      // BUG: Diagnostic contains: (Null)",
            "      triggerNullnessChecker(o.f);",
            "    } else {",
            "      // BUG: Diagnostic contains: (Non-null)",
            "      triggerNullnessChecker(o.f);",
            "    }",
            "  }",
            "  abstract class Coinductive {",
            "    Coinductive f;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAccessPaths() {
    compilationHelper
        .addSourceLines(
            "AccessPathsTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "public class AccessPathsTest {",
            "  public static void access_paths(Coinductive o) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(o.f.f.f.f.f);",
            "    o.f.f.f.f.f = (Coinductive) new Object();",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(o.f.f.f.f.f);",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(o.f.f.f.f);",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(o.f.f.f);",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(o.f.f);",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(o.f);",
            "  }",
            "  abstract class Coinductive {",
            "    Coinductive f;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testUntrackableFields() {
    compilationHelper
        .addSourceLines(
            "UntrackableFieldsTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "public class UntrackableFieldsTest {",
            "  public static void untrackable_fields(CoinductiveWithMethod o) {",
            "    o.f.f = (CoinductiveWithMethod) new Object();",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(o.f.f);",
            "    o.foo().f = (CoinductiveWithMethod) new Object();",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(o.foo().f);",
            "  }",
            "  abstract class CoinductiveWithMethod {",
            "    CoinductiveWithMethod f;",
            "    abstract CoinductiveWithMethod foo();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAnnotatedAtGenericTypeUse() {
    compilationHelper
        .addSourceLines(
            "AnnotatedAtGenericTypeUseTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class AnnotatedAtGenericTypeUseTest {",
            "  void test(MyInnerClass<@Nullable Object> nullable,"
                + "MyInnerClass<@NonNull Object> nonnull) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nullable.get());",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(nonnull.get());",
            "  }",
            "  interface MyInnerClass<T> {",
            "    T get();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAnnotatedAtGenericTypeDef() {
    compilationHelper
        .addSourceLines(
            "AnnotatedAtGenericTypeDefTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "import org.checkerframework.checker.nullness.compatqual.NullableDecl;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class AnnotatedAtGenericTypeDefTest {",
            "  void test(NullableTypeInner<?> nullable,"
                + "NonNullTypeInner<?> nonnull,"
                + "TypeAndBoundInner<?> type_and_bound) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nullable.get());",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(nullable.getNonNull());",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(nonnull.get());",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nonnull.getNullable());",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nonnull.getNullableDecl());",
            "    // BUG: Diagnostic contains: (Nullable)", // use upper bound (b/121398981)
            "    triggerNullnessChecker(type_and_bound.get());",
            "  }",
            "  interface NullableTypeInner<@Nullable T> {",
            "    T get();",
            "    @NonNull T getNonNull();",
            "  }",
            "  interface NonNullTypeInner<@NonNull T> {",
            "    T get();",
            "    @Nullable T getNullable();",
            "    @NullableDecl T getNullableDecl();",
            "  }",
            "  interface TypeAndBoundInner<@NonNull T extends @Nullable Object> { T get(); }",
            "}")
        .doTest();
  }

  @Test
  public void testBoundedAtGenericTypeUse() {
    compilationHelper
        .addSourceLines(
            "BoundedAtGenericTypeUseTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class BoundedAtGenericTypeUseTest {",
            "  void test(MyInnerClass<? extends @Nullable Object> nullable,"
                + "MyInnerClass<? extends @NonNull Object> nonnull) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nullable.get());",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(nullable.getNonNull());",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(nonnull.get());",
            "  }",
            "  interface MyInnerClass<T> {",
            "    T get();",
            "    @NonNull T getNonNull();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testBoundedAtGenericTypeDef() {
    compilationHelper
        .addSourceLines(
            "BoundedAtGenericTypeDefTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class BoundedAtGenericTypeDefTest {",
            "  void test(NullableElementCollection<?> nullable, "
                + "NonNullElementCollection<?> nonnull) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nullable.get());",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(nonnull.get());",
            "  }",
            "  interface NullableElementCollection<T extends @Nullable Object> {",
            "    T get();",
            "  }",
            "  interface NonNullElementCollection<T extends @NonNull Object> {",
            "    T get();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAnnotatedMethodTypeParams() {
    compilationHelper
        .addSourceLines(
            "AnnotatedMethodTypeParamsTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class AnnotatedMethodTypeParamsTest {",
            "  public void test() {",
            "    Object o = new Object();",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(AnnotatedMethodTypeParamsTest.<@NonNull Object>id(o));",
            "  }",
            "  static <T> T id(T t) { return t; }",
            "}")
        .doTest();
  }

  @Test
  public void testFieldAnnotations() {
    compilationHelper
        .addSourceLines(
            "FieldAnnotationsTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "import org.checkerframework.checker.nullness.compatqual.NullableDecl;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class FieldAnnotationsTest {",
            "  void test(NullableTypeInner<?> nullable,"
                + "NonNullTypeInner<?> nonnull,"
                + "TypeAndBoundInner<?> type_and_bound) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nullable.foo);",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(nonnull.foo);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nonnull.fooNullable);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nonnull.fooNullableDecl);",
            "    // BUG: Diagnostic contains: (Nullable)", // use upper bound (b/121398981)
            "    triggerNullnessChecker(type_and_bound.foo);",
            "  }",
            "  class NullableTypeInner<@Nullable T> { T foo; }",
            "  class NonNullTypeInner<@NonNull T> {",
            "    T foo;",
            "    @Nullable T fooNullable;",
            "    @NullableDecl T fooNullableDecl;",
            "  }",
            "  class TypeAndBoundInner<@NonNull T extends @Nullable Object> { T foo; }",
            "}")
        .doTest();
  }

  @Test
  public void testCheckerWorksInsideLambdaBody() {
    compilationHelper
        .addSourceLines(
            "LambdaBodyTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "public class LambdaBodyTest {",
            "  public void startNothing() {",
            "    new Thread(()",
            "            // BUG: Diagnostic contains: (Null)",
            "            -> triggerNullnessChecker(null))",
            "        .start();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testCheckerWorksInsideInitializer() {
    compilationHelper
        .addSourceLines(
            "InitializerBlockTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "public class InitializerBlockTest {",
            "  // BUG: Diagnostic contains: (Null)",
            "  Object o1 = triggerNullnessChecker(null);", // instance field initializer
            "  // BUG: Diagnostic contains: (Null)",
            "  static Object o2 = triggerNullnessChecker(null);", // static field initializer
            "  {", // instance initializer block
            "    // BUG: Diagnostic contains: (Null)",
            "    triggerNullnessChecker(null);",
            "  }",
            "  static {", // static initializer block
            "    // BUG: Diagnostic contains: (Null)",
            "    triggerNullnessChecker(null);",
            "    // The following is a regression test for b/80179088",
            "    int i = 0; i = i++;",
            "  }",
            "}")
        .doTest();
  }

  /**
   * Tests nullness propagation for references to constants defined in other compilation units. Enum
   * constants and compile-time constants are still known to be non-null; other constants are
   * assumed nullable. It doesn't matter whether the referenced compilation unit is part of the same
   * compilation or not. Note we often do better when constants are defined in the same compilation
   * unit. Circular initialization dependencies between compilation units are also not recognized
   * while we do recognize them inside a compilation unit.
   */
  @Test
  public void testConstantsDefinedInOtherCompilationUnits() {
    compilationHelper
        .addSourceLines(
            "AnotherEnum.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "public enum AnotherEnum {",
            "  INSTANCE;",
            "  public static final String COMPILE_TIME_CONSTANT = \"not null\";",
            "  public static final AnotherEnum NOT_COMPILE_TIME_CONSTANT = INSTANCE;",
            "  public static final String CIRCULAR = ConstantsFromOtherCompilationUnits.CIRCULAR;",
            "}")
        .addSourceLines(
            "ConstantsFromOtherCompilationUnits.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "public class ConstantsFromOtherCompilationUnits {",
            "  public static final String CIRCULAR = AnotherEnum.CIRCULAR;",
            "  public void referenceInsideCompilation() {",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(AnotherEnum.INSTANCE);",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(AnotherEnum.COMPILE_TIME_CONSTANT);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(AnotherEnum.NOT_COMPILE_TIME_CONSTANT);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(CIRCULAR);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(AnotherEnum.CIRCULAR);",
            "  }",
            "",
            "  public void referenceOutsideCompilation() {",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(NullnessPropagationTest.COMPILE_TIME_CONSTANT);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(NullnessPropagationTest.NOT_COMPILE_TIME_CONSTANT);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(System.out);",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(java.math.RoundingMode.UNNECESSARY);",
            "  }",
            "}")
        .doTest();
  }

  // Regression test for b/110756716, verifying that the l-val of an assignment in expr position in
  // an equality comparison is refined
  @Test
  public void testWhileLoopPartialCorrectness() {
    compilationHelper
        .addSourceLines(
            "PartialCorrectnessTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "public class PartialCorrectnessTest {",
            "  public void test(java.util.function.Supplier<Object> supplier) {",
            "    Object result;",
            "    while((result = supplier.get()) == null) { }",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(result);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testCasts() {
    compilationHelper
        .addSourceLines(
            "CastsTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class CastsTest {",
            "  public void test(@Nullable Object o) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(o);",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker((@NonNull Object) o);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAutoValue() {
    compilationHelper
        .addSourceLines(
            "AutoValueTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "import com.google.auto.value.AutoValue;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class AutoValueTest {",
            "  public void test(Value v) {",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(v.accessor().field);",
            "    if (v.accessor().field != null) {",
            "      // BUG: Diagnostic contains: (Non-null)",
            "      triggerNullnessChecker(v.accessor().field);",
            "    }",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(v.nullableAccessor());",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(v.accessor());",
            "  }",
            "  @AutoValue",
            "  static abstract class Value {",
            "    Value field;",
            "    abstract Value accessor();",
            "    @Nullable abstract Value nullableAccessor();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testGenericTypeInference() {
    compilationHelper
        .addSourceLines(
            "GenericTypeInferenceTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class GenericTypeInferenceTest {",
            "  public void test(@NonNull Object o) {",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(o);",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(id(o));",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(idButMaybeNullify(o));",
            "  }",
            "  <T> T id(T t) {return t;}",
            "  <T> @Nullable T idButMaybeNullify(T t) {",
            "    return java.lang.Math.random() > 0.5 ? t : null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAnnotatedFormal() {
    compilationHelper
        .addSourceLines(
            "AnnotatedFormalTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessPropagationTest.triggerNullnessChecker;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class AnnotatedFormalTest {",
            "  public void test(@NonNull Object nonnull, @Nullable Object nullable, Object o) {",
            "    // BUG: Diagnostic contains: (Non-null)",
            "    triggerNullnessChecker(nonnull);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(nullable);",
            "    // BUG: Diagnostic contains: (Nullable)",
            "    triggerNullnessChecker(o);",
            "  }",
            "}")
        .doTest();
  }

  /** BugPattern to test dataflow analysis using nullness propagation */
  @BugPattern(
      summary = "Test checker for NullnessPropagationTest",
      explanation =
          "Outputs an error for each call to triggerNullnessChecker, describing its "
              + "argument as nullable or non-null",
      severity = ERROR)
  public static final class NullnessPropagationChecker extends BugChecker
      implements MethodInvocationTreeMatcher {
    private final NullnessPropagationTransfer nullnessPropagation =
        new NullnessPropagationTransfer();

    private static final String AMBIGUOUS_CALL_MESSAGE =
        "AMBIGUOUS CALL: use "
            + "triggerNullnessCheckerOnPrimitive if you want to test the primitive for nullness";

    private static final Matcher<ExpressionTree> TRIGGER_CALL_MATCHER =
        anyOf(
            staticMethod()
                .onClass(NullnessPropagationTest.class.getName())
                .named("triggerNullnessCheckerOnPrimitive"),
            staticMethod()
                .onClass(NullnessPropagationTest.class.getName())
                .named("triggerNullnessCheckerOnBoxed"),
            staticMethod()
                .onClass(NullnessPropagationTest.class.getName())
                .named("triggerNullnessChecker")
                .withParameters("java.lang.Object"));

    private static final Matcher<ExpressionTree> AMBIGUOUS_CALL_FALLBACK_MATCHER =
        staticMethod()
            .onClass(NullnessPropagationTest.class.getName())
            .named("triggerNullnessChecker");

    @Override
    public Description matchMethodInvocation(
        MethodInvocationTree methodInvocation, VisitorState state) {
      if (!TRIGGER_CALL_MATCHER.matches(methodInvocation, state)) {
        if (AMBIGUOUS_CALL_FALLBACK_MATCHER.matches(methodInvocation, state)) {
          return buildDescription(methodInvocation).setMessage(AMBIGUOUS_CALL_MESSAGE).build();
        }
        return NO_MATCH;
      }

      TreePath root = state.getPath();
      List<Object> values = new ArrayList<>();
      for (Tree arg : methodInvocation.getArguments()) {
        TreePath argPath = new TreePath(root, arg);
        nullnessPropagation.setContext(state.context).setCompilationUnit(root.getCompilationUnit());
        values.add(expressionDataflow(argPath, state.context, nullnessPropagation));
        nullnessPropagation.setContext(null).setCompilationUnit(null);
      }

      String fixString = "(" + Joiner.on(", ").join(values) + ")";
      return describeMatch(methodInvocation, replace(methodInvocation, fixString));
    }
  }
}
