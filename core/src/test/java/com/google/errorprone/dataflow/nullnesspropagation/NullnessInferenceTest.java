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

package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.inference.InferredNullability;
import com.google.errorprone.dataflow.nullnesspropagation.inference.NullnessQualifierInference;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author bennostein@google.com (Benno Stein)
 */
@RunWith(JUnit4.class)
public class NullnessInferenceTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(NullnessInferenceChecker.class, getClass());

  /**
   * This method triggers the {@code BugPattern} used to test nullness annotation inference
   *
   * @param t Method call whose type arguments' inferred nullness will be inspected
   * @return {@code t} for use in expressions
   */
  public static <T> T inspectInferredGenerics(T t) {
    return t;
  }

  /**
   * This method triggers the {@code BugPattern} used to test nullness annotation inference
   *
   * @param t Expression whose inferred nullness will be inspected
   * @return {@code t} for use in expressions
   */
  public static <T> T inspectInferredExpression(T t) {
    return t;
  }

  @Test
  public void testIdentity() {
    compilationHelper
        .addSourceLines(
            "IdentityTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredGenerics;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class IdentityTest {",
            "  @Nullable Object nullableObj;",
            "  @NonNull Object nonnullObj;",
            "  <T> T id(T t) { return t; }",
            "  void id_tests() {",
            "    // BUG: Diagnostic contains: {T=Nullable}",
            "    inspectInferredGenerics(id(nullableObj));",
            "    // BUG: Diagnostic contains: {T=Non-null}",
            "    inspectInferredGenerics(id(nonnullObj));",
            "  }",
            "  void literal_tests() {",
            "    // BUG: Diagnostic contains: {T=Null}",
            "    inspectInferredGenerics(id(null));",
            "    // BUG: Diagnostic contains: {T=Non-null}",
            "    inspectInferredGenerics(id(this));",
            "    // BUG: Diagnostic contains: {T=Non-null}",
            "    inspectInferredGenerics(id(5));",
            "    // BUG: Diagnostic contains: {T=Non-null}",
            "    inspectInferredGenerics(id(\"hello\"));",
            "    // BUG: Diagnostic contains: {T=Non-null}",
            "    inspectInferredGenerics(id(new Object()));",
            "    // BUG: Diagnostic contains: {T=Non-null}",
            "    inspectInferredGenerics(id(new Object[0]));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAnnotatedGenericMethod() {
    compilationHelper
        .addSourceLines(
            "AnnotatedGenericMethodTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredExpression;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredGenerics;",
            "import org.checkerframework.checker.nullness.compatqual.NullableDecl;",
            "import org.checkerframework.checker.nullness.compatqual.NonNullDecl;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class AnnotatedGenericMethodTest {",
            "  <T> @NonNull T makeNonNull(@Nullable T t) { return t; }",
            "  <T> @NonNullDecl T makeNonNullDecl(@NullableDecl T t) { return t; }",
            "  void test_type_anno(Object o) {",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    makeNonNull(inspectInferredExpression(o));",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(makeNonNull(o));",
            "  }",
            "  void test_decl_anno(Object o) {",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    makeNonNullDecl(inspectInferredExpression(o));",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(makeNonNullDecl(o));",
            "  }",
            "  void test_generics(Object o) {",
            "    // BUG: Diagnostic contains: {}",
            "    inspectInferredGenerics(makeNonNull(o));",
            "    // BUG: Diagnostic contains: {}",
            "    inspectInferredGenerics(makeNonNullDecl(o));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testBoundedGenericMethod() {
    compilationHelper
        .addSourceLines(
            "AnnotatedGenericMethodTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredExpression;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredGenerics;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class AnnotatedGenericMethodTest {",
            "  <T extends @NonNull Object> T requireNonNull(T t) { return t; }",
            "  <T extends @NonNull Object> T makeNonNull(@Nullable T t) { return t; }",
            "  void test_bound_only(Object o) {",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(requireNonNull(o));",
            "    // BUG: Diagnostic contains: {T=Non-null}",
            "    inspectInferredGenerics(requireNonNull(o));",
            "  }",
            "  void test_bound_and_param_anno(Object o) {",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(makeNonNull(o));",
            "    // BUG: Diagnostic contains: {T=Non-null}",
            "    inspectInferredGenerics(makeNonNull(o));",
            "  }",
            "}")
        .doTest();
  }

  // Regression test for b/113123074
  @Test
  public void testUnparameterizedMethodInvocation() {
    compilationHelper
        .addSourceLines(
            "IdentityTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredExpression;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class IdentityTest {",
            "  @NonNull Object returnNonnull(@Nullable Integer i) { return \"hello\"; }",
            "  @Nullable Object returnNullable(@Nullable Integer i) { return \"hello\"; }",
            "  Object returnUnannotated(@Nullable Integer i) { return \"hello\"; }",
            "  void invoke_test() {",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(returnNonnull(null));",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(returnNullable(null));",
            "    // BUG: Diagnostic contains: Optional.empty",
            "    inspectInferredExpression(returnUnannotated(\"\".hashCode()));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testLists() {
    compilationHelper
        .addSourceLines(
            "ListsTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredGenerics;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredExpression;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class ListsTest {",
            "  @Nullable Object nullableObj;",
            "  @NonNull Object nonnullObj;",
            "  void lists_tests(Object o1, Object o2) {",
            // Infer from target type (if possible)
            "    // BUG: Diagnostic contains: {}",
            "    inspectInferredGenerics(List.cons(o1, List.cons(o2, List.nil())));",
            "    // BUG: Diagnostic contains: {Z=Non-null}",
            "    List<@NonNull Object> l = "
                + "inspectInferredGenerics(List.cons(o1, List.cons(o2, List.nil())));",

            // Infer from argument types (if possible)
            "    // BUG: Diagnostic contains: {Z=Non-null}",
            "    inspectInferredGenerics(List.cons(nonnullObj, List.<@NonNull Object>nil()));",
            "    // BUG: Diagnostic contains: {Z=Non-null}",
            "    inspectInferredGenerics(List.cons(nonnullObj, List.<Object>nil()));",
            "    // BUG: Diagnostic contains: {Z=Nullable}",
            "    inspectInferredGenerics("
                + "List.cons(nullableObj, List.cons(nonnullObj, List.nil())));",

            // Propagate inference about receiver to result of calls
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression("
                + "List.cons(nonnullObj, List.<@NonNull Object>nil()).head());",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(List.cons(nonnullObj, List.<Object>nil()).head());",
            "  }",
            "}",
            "  abstract class List<T> {",
            "    abstract T head();",
            "    static <X> List<X> nil() {return null;}",
            "    static <Z> List<Z> cons(Z z, List<Z> zs ) {return null;}",
            "  }")
        .doTest();
  }

  @Test
  public void testReturn() {
    compilationHelper
        .addSourceLines(
            "ReturnTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredGenerics;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredExpression;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class ReturnTest {",
            "  List<@NonNull Object> return_tests(Object o1, Object o2) {",
            "    // BUG: Diagnostic contains: {}",
            "    inspectInferredGenerics(List.cons(o1, List.cons(o2, List.nil())));",
            "    // BUG: Diagnostic contains: {Z=Non-null}",
            "    return inspectInferredGenerics(List.cons(o1, List.cons(o2, List.nil())));",
            "  }",
            "}",
            "abstract class List<T> {",
            "  abstract T head();",
            "  static <X> List<X> nil() {return null;}",
            "  static <Z> List<Z> cons(Z z, List<Z> zs ) {return null;}",
            "}")
        .doTest();
  }

  @Test
  public void testAssignments() {
    compilationHelper
        .addSourceLines(
            "AssignmentsTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredGenerics;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredExpression;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class AssignmentsTest {",
            "  void assignments_tests(@Nullable Object nullable, Object unknown) {",
            "    // BUG: Diagnostic contains: {}",
            "    inspectInferredGenerics(List.cons(unknown, List.nil()));",
            "    // BUG: Diagnostic contains: {Z=Non-null}",
            "    List<@NonNull Object> a ="
                + " inspectInferredGenerics(List.cons(unknown, List.nil()));",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    @NonNull Object b ="
                + " inspectInferredExpression(List.cons(unknown, List.nil()).head());",
            "  }",
            "}",
            "abstract class List<T> {",
            "  abstract T head();",
            "  static <X> List<X> nil() {return null;}",
            "  static <Z> List<Z> cons(Z z, List<Z> zs ) {return null;}",
            "}")
        .doTest();
  }

  @Test
  public void testVarArgs() {
    compilationHelper
        .addSourceLines(
            "VarArgsTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredGenerics;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredExpression;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "public class VarArgsTest {",
            "  void foo(@NonNull Object... objects) {}",
            "  void bar(@Nullable Object... objects) {}",
            "  void varargs_tests(Object o) {",
            "    // BUG: Diagnostic contains: Optional.empty",
            "    inspectInferredExpression(o);",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    foo(inspectInferredExpression(o));",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    foo(o, o, o, inspectInferredExpression(o));",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    bar(inspectInferredExpression(o));",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    bar(o, o, o, inspectInferredExpression(o));",
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
                + "NullnessInferenceTest.inspectInferredExpression;",
            "import org.checkerframework.checker.nullness.compatqual.NullableDecl;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class AnnotatedAtGenericTypeUseTest {",
            "  void test(MyInnerClass<@Nullable Object> nullable, "
                + "MyInnerClass<@NonNull Object> nonnull) {",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(nullable.get());",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(nullable.getNonNull());",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(nonnull.get());",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(nonnull.getNullable());",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(nonnull.getNullableDecl());",
            "  }",
            "  void testNested_nullable(MyWrapper<MyInnerClass<@Nullable Object>> nullable) {",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(nullable.get().get());",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(nullable.get().getNonNull());",
            "  }",
            "  void testNested_nonnull(MyWrapper<MyInnerClass<@NonNull Object>> nonnull) {",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(nonnull.get().get());",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(nonnull.get().getNullable());",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(nonnull.get().getNullableDecl());",
            "  }",
            "  interface MyInnerClass<T> {",
            "    T get();",
            "    @NonNull T getNonNull();",
            "    @Nullable T getNullable();",
            "    @NullableDecl T getNullableDecl();",
            "  }",
            "  interface MyWrapper<T> {",
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
                + "NullnessInferenceTest.inspectInferredExpression;",
            "import org.checkerframework.checker.nullness.compatqual.NullableDecl;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class AnnotatedAtGenericTypeDefTest {",
            "  void test(NullableTypeInner<?> nullable) {",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(nullable.get());",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(nullable.getNonNull());",
            "  }",
            "  void test(NonNullTypeInner<?> nonnull) {",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(nonnull.get());",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(nonnull.getNullable());",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(nonnull.getNullableDecl());",
            "  }",
            "  void test(TypeAndBoundInner<?> type_and_bound) {",
            "    // BUG: Diagnostic contains: Optional[Nullable]", // use upper bound (b/121398981)
            "    inspectInferredExpression(type_and_bound.get());",
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
                + "NullnessInferenceTest.inspectInferredExpression;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class BoundedAtGenericTypeUseTest {",
            "  void test(MyInnerClass<? extends @Nullable Object> nullable,"
                + "MyInnerClass<? extends @NonNull Object> nonnull) {",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(nullable.get());",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(nonnull.get());",
            "  }",
            "  interface MyInnerClass<T> {",
            "    T get();",
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
                + "NullnessInferenceTest.inspectInferredExpression;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class BoundedAtGenericTypeDefTest {",
            "  void test(NullableElementCollection<?> nullable) {",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(nullable.get());",
            "  }",
            "  void test(NonNullElementCollection<?> nonnull) {",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(nonnull.get());",
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
  public void testDefaultAnnotation() {
    compilationHelper
        .addSourceLines(
            "DefaultAnnotationTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredExpression;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class DefaultAnnotationTest<T> {",
            "  public void testGenericMethod() {",
            "    Object o = new Object();",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(defaulted());",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(boundDefaulted());",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(bounded());",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(explicit(o));",
            "  }",
            "  @DefaultNotNull void testParameters(T undefaulted, String defaulted,",
            "      @Nullable Integer explicit) {",
            "    // BUG: Diagnostic contains: Optional.empty", // because T is declared elsewhere
            "    inspectInferredExpression(undefaulted);",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(defaulted);",
            "    // BUG: Diagnostic contains: Optional[Nullable]",
            "    inspectInferredExpression(explicit);",
            "  }",
            "  @DefaultNotNull static Object defaulted() { return null; }",
            "  @DefaultNotNull static <T> T boundDefaulted() { return null; }",
            "  @DefaultNotNull static <T extends @Nullable Object> T bounded() { return null; }",
            "  @DefaultNotNull static <T> @Nullable T explicit(T t) { return t; }",
            "  public @interface DefaultNotNull {}",
            "}")
        .doTest();
  }

  @Test
  public void testIntersectionBounds() {
    compilationHelper
        .addSourceLines(
            "IntersectionBoundsTest.java",
            "package com.google.errorprone.dataflow.nullnesspropagation;",
            "import static com.google.errorprone.dataflow.nullnesspropagation."
                + "NullnessInferenceTest.inspectInferredExpression;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class IntersectionBoundsTest {",
            "  void test(MyBoundedClass<?> bounded) {",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(bounded.get());",
            "  }",
            "  interface MyBoundedClass<T extends @NonNull Number & @Nullable Iterable> {",
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
                + "NullnessInferenceTest.inspectInferredExpression;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class AnnotatedMethodTypeParamsTest {",
            "  public void test() {",
            "    Object o = new Object();",
            "    // BUG: Diagnostic contains: Optional[Non-null]",
            "    inspectInferredExpression(AnnotatedMethodTypeParamsTest.<@NonNull Object>id(o));",
            "  }",
            "  static <T> T id(T t) { return t; }",
            "}")
        .doTest();
  }

  /** BugPattern to test inference of nullness qualifiers */
  @BugPattern(
      summary = "Test checker for NullnessInferenceTest",
      explanation =
          "Outputs an error for each call to inspectInferredExpression and inspectInferredGenerics,"
              + " displaying inferred or explicitly-specified Nullness information.",
      severity = ERROR)
  public static final class NullnessInferenceChecker extends BugChecker
      implements MethodInvocationTreeMatcher {

    private static final Matcher<ExpressionTree> GENERICS_CALL_MATCHER =
        staticMethod()
            .onClass(NullnessInferenceTest.class.getName())
            .named("inspectInferredGenerics");

    private static final Matcher<ExpressionTree> EXPRESSION_CALL_MATCHER =
        staticMethod()
            .onClass(NullnessInferenceTest.class.getName())
            .named("inspectInferredExpression");

    @Override
    public Description matchMethodInvocation(
        MethodInvocationTree methodInvocation, VisitorState state) {
      if (GENERICS_CALL_MATCHER.matches(methodInvocation, state)) {
        TreePath root = state.getPath();
        InferredNullability inferenceRes =
            NullnessQualifierInference.getInferredNullability(
                ASTHelpers.findEnclosingNode(root, MethodTree.class));
        assertThat(methodInvocation.getArguments().get(0).getKind())
            .isEqualTo(Kind.METHOD_INVOCATION);
        MethodInvocationTree callsiteToInspect =
            (MethodInvocationTree) methodInvocation.getArguments().get(0);
        return describeMatch(
            callsiteToInspect,
            replace(
                methodInvocation, inferenceRes.getNullnessGenerics(callsiteToInspect).toString()));
      } else if (EXPRESSION_CALL_MATCHER.matches(methodInvocation, state)) {
        TreePath root = state.getPath();
        InferredNullability inferenceRes =
            NullnessQualifierInference.getInferredNullability(
                ASTHelpers.findEnclosingNode(root, MethodTree.class));
        ExpressionTree exprToInspect = methodInvocation.getArguments().get(0);
        return describeMatch(
            exprToInspect,
            replace(methodInvocation, inferenceRes.getExprNullness(exprToInspect).toString()));
      } else {
        return NO_MATCH;
      }
    }
  }
}
