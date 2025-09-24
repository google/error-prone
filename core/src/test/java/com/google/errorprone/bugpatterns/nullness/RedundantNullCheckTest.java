/*
 * Copyright 2025 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public class RedundantNullCheckTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(RedundantNullCheck.class, getClass());

  @Test
  public void positive_equalsNull_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test {",
            "  void foo(String s) {",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    if (s == null) {",
            "      System.out.println(\"s is null\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_notEqualsNull_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test {",
            "  void foo(String s) {",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    if (s != null) {",
            "      System.out.println(\"s is not null\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_explicitlyNullable_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import org.jspecify.annotations.Nullable;",
            "@NullMarked",
            "class Test {",
            "  void foo(@Nullable String s) {",
            "    if (s == null) { /* This is fine */ }",
            "    if (s != null) { /* This is fine */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_outsideNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void foo(String s) {",
            "    if (s == null) { /* This is fine */ }",
            "    if (s != null) { /* This is fine */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_classMarkedAndUnmarked_effectivelyUnmarked() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import org.jspecify.annotations.NullUnmarked;",
            "@NullMarked",
            "@NullUnmarked", // This should override @NullMarked for the class scope
            "class Test {",
            "  void foo(String s) {",
            "    if (s == null) { /* This is fine */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_packageNullMarked_classNullUnmarked() {
    compilationHelper
        .addSourceLines(
            "foo/package-info.java",
            "@org.jspecify.annotations.NullMarked",
            "package foo;")
        .addSourceLines(
            "foo/Test.java",
            "package foo;",
            "import org.jspecify.annotations.NullUnmarked;",
            "@NullUnmarked",
            "class Test {",
            "  void foo(String s) {",
            "    if (s == null) { /* This is fine */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_packageNullMarked_classNotAnnotated() {
    compilationHelper
        .addSourceLines(
            "foo/package-info.java",
            "@org.jspecify.annotations.NullMarked",
            "package foo;")
        .addSourceLines(
            "foo/Test.java",
            "package foo;",
            "class Test {",
            "  void foo(String s) {",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    if (s == null) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_packageNullMarked_methodNullUnmarked() {
    compilationHelper
        .addSourceLines(
            "foo/package-info.java",
            "@org.jspecify.annotations.NullMarked",
            "package foo;")
        .addSourceLines(
            "foo/Test.java",
            "package foo;",
            "import org.jspecify.annotations.NullUnmarked;",
            "class Test {",
            "  @NullUnmarked",
            "  void foo(String s) {",
            "    if (s == null) { /* This is fine */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_field_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test {",
            "  String f;",
            "  void foo() {",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    if (f == null) {",
            "      System.out.println(\"f is null\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_field_explicitlyNullable_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import org.jspecify.annotations.Nullable;",
            "@NullMarked",
            "class Test {",
            "  @Nullable String f;",
            "  void foo() {",
            "    if (f == null) { /* This is fine */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_variableInitializedFromUnannotatedMethod_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "UnannotatedLib.java", // Separate file, not @NullMarked
            "class UnannotatedLib {",
            "  public static String getString() { return null; }",
            "}")
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test {",
            "  void process() {",
            "    String result = UnannotatedLib.getString();",
            "    if (result == null) { /* This check should NOT be redundant */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_variableInitializedFromMapGet_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import java.util.Map;",
            "@NullMarked",
            "class Test {",
            "  void process(Map<String, String> map, String key) {",
            "    String value = map.get(key);",
            "    if (value == null) { /* This check should NOT be redundant */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_variableInitializedFromAnnotatedLib_returnNullable_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "AnnotatedLibNullable.java",
            "package mylib;",
            "import org.jspecify.annotations.NullMarked;",
            "import org.jspecify.annotations.Nullable;",
            "@NullMarked",
            "public class AnnotatedLibNullable {",
            "  public static @Nullable String getString() { return null; }",
            "}")
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import mylib.AnnotatedLibNullable;",
            "@NullMarked",
            "class Test {",
            "  void process() {",
            "    String result = AnnotatedLibNullable.getString();",
            "    if (result == null) { /* This check should NOT be redundant */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_variableInitializedFromNonAnnotatedLib_returnNonNull_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "NonAnnotatedLibNonNull.java", // Not @NullMarked
            "package mylib;",
            "import org.jspecify.annotations.NonNull;",
            "public class NonAnnotatedLibNonNull {",
            // Even though the lib is not @NullMarked, the @NonNull is explicit
            "  public static @NonNull String getString() { return \"non-null\"; }",
            "}")
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import mylib.NonAnnotatedLibNonNull;",
            "@NullMarked",
            "class Test {",
            "  void process() {",
            "    String result = NonAnnotatedLibNonNull.getString();",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    if (result == null) { }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_localVariable_implicitType_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test {",
            "  String getString() { return \"foo\"; }",
            "  void process() {",
            "    var s = getString();",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    if (s == null) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_methodCall_inNullMarkedScope_defaultNonNullReturn() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test {",
            "  String getString() { return \"foo\"; }",
            "  void process() {",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    if (getString() == null) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_methodCall_inNullMarkedScope_explicitlyNullableReturn() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import org.jspecify.annotations.Nullable;",
            "@NullMarked",
            "class Test {",
            "  @Nullable String getNullableString() { return null; }",
            "  void process() {",
            "    if (getNullableString() == null) { /* This is fine */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_methodCall_inNullMarkedScope_explicitlyNonNullReturn() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import org.jspecify.annotations.NonNull;",
            "@NullMarked",
            "class Test {",
            "  @NonNull String getNonNullString() { return \"foo\"; }",
            "  void process() {",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    if (getNonNullString() == null) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_methodCall_fromUnannotatedLib_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "UnannotatedLib.java", // Separate file, not @NullMarked
            "class UnannotatedLib {",
            "  public static String getString() { return null; }",
            "}")
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test {",
            "  void process() {",
            "    if (UnannotatedLib.getString() == null) { /* This check should NOT be redundant */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_methodCall_fromMapGet_inNullMarkedScope() {
    // This test is similar to variableInitializedFromMapGet, but checks direct method call
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import java.util.Map;",
            "@NullMarked",
            "class Test {",
            "  void process(Map<String, String> map, String key) {",
            "    if (map.get(key) == null) { /* This check should NOT be redundant */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_methodCall_fromAnnotatedLib_returnNullable_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "AnnotatedLibNullable.java",
            "package mylib;",
            "import org.jspecify.annotations.NullMarked;",
            "import org.jspecify.annotations.Nullable;",
            "@NullMarked", // Library itself is null-marked
            "public class AnnotatedLibNullable {",
            "  public static @Nullable String getString() { return null; }",
            "}")
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import mylib.AnnotatedLibNullable;",
            "@NullMarked",
            "class Test {",
            "  void process() {",
            "    if (AnnotatedLibNullable.getString() == null) { /* This check should NOT be redundant */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_methodCall_fromNonAnnotatedLib_returnNonNull_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "NonAnnotatedLibNonNull.java", // Not @NullMarked
            "package mylib;",
            "import org.jspecify.annotations.NonNull;",
            "public class NonAnnotatedLibNonNull {",
            "  public static @NonNull String getString() { return \"non-null\"; }",
            "}")
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import mylib.NonAnnotatedLibNonNull;",
            "@NullMarked",
            "class Test {",
            "  void process() {",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    if (NonAnnotatedLibNonNull.getString() == null) { }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_methodCall_fromNullUnmarkedClass_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "NullUnmarkedLib.java",
            "package mylib;",
            "import org.jspecify.annotations.NullUnmarked;",
            "@NullUnmarked",
            "public class NullUnmarkedLib {",
            "  public static String getString() { return null; }", // Implicitly nullable return
            "}")
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import mylib.NullUnmarkedLib;",
            "@NullMarked",
            "class Test {",
            "  void process() {",
            "    if (NullUnmarkedLib.getString() == null) { /* This is fine */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_methodCall_onInstance_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test {",
            "  class Greeter { String greet() { return \"hello\"; } }",
            "  void process() {",
            "    Greeter greeter = new Greeter();",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    if (greeter.greet() == null) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_constructorCall_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test {",
            "  void foo() {",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    if (new Object() == null) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_methodCall_onInstance_explicitlyNullableReturn_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import org.jspecify.annotations.Nullable;",
            "@NullMarked",
            "class Test {",
            "  class Greeter { @Nullable String greet() { return null; } }",
            "  void process() {",
            "    Greeter greeter = new Greeter();",
            "    if (greeter.greet() == null) { /* This is fine */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_genericTypeParameter_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test<T> {",
            "  void foo(T t) {",
            "    if (t == null) { /* This is fine, T could be nullable */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_inferredLambdaParameter_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import java.util.function.Consumer;",
            "@NullMarked",
            "class Test {",
            "  void foo() {",
            "    Consumer<String> consumer = s -> {",
            "      if (s == null) { /* This is fine, inferred type might be nullable */ }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_nonInferredLambdaParameter_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import java.util.function.Consumer;",
            "@NullMarked",
            "class Test {",
            "  void foo() {",
            "    Consumer<String> consumer = (String s) -> {",
            "      // BUG: Diagnostic contains: RedundantNullCheck",
            "      if (s == null) {}",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_nonInferredNullableLambdaParameter_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.Nullable;",
            "import org.jspecify.annotations.NullMarked;",
            "import java.util.function.Consumer;",
            "@NullMarked",
            "class Test {",
            "  void foo() {",
            "    Consumer<String> consumer = (@Nullable String s) -> {",
            "      if (s == null) { /* This is fine, s is nullable */ }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_nonFinalLocalVariable_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test {",
            "  String getString() { return \"foo\"; }",
            "  void foo(boolean b) {",
            "    String s = getString();",
            "    if (b) {",
            "      s = null; // s is not effectively final",
            "    }",
            "    if (s == null) { /* This is fine */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_localVariable_annotatedNullable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.Nullable;",
            "class Test {",
            "  void foo() {",
            "    @Nullable String s = \"hello\";",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    if (s == null) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_variableInitializedWithLiteral_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test {",
            "  void foo() {",
            "    String s = \"hello\";",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    if (s == null) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_variableInitializedWithNullLiteral_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test {",
            "  void foo() {",
            "    String s = null;",
            "    if (s == null) { /* This is fine */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_methodCall_returnsGenericType_inNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Test<T> {",
            "  T get() { return null; }",
            "  void foo() {",
            "    if (get() == null) { /* This is fine, T could be nullable */ }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_objectsRequireNonNull_inNullMarkedScope() {
    compilationHelper
        .setArgs("-XepOpt:RedundantNullCheck:CheckRequireNonNull=true")
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import java.util.Objects;",
            "@NullMarked",
            "class Test {",
            "  void foo(String s) {",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    Objects.requireNonNull(s);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_objectsRequireNonNull_methodCall_inNullMarkedScope() {
    compilationHelper
        .setArgs("-XepOpt:RedundantNullCheck:CheckRequireNonNull=true")
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import java.util.Objects;",
            "@NullMarked",
            "class Test {",
            "  String getString() { return \"foo\"; }",
            "  void foo() {",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    Objects.requireNonNull(getString());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_objectsRequireNonNull_byDefault() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import java.util.Objects;",
            "@NullMarked",
            "class Test {",
            "  void foo(String s) {",
            "    Objects.requireNonNull(s);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_objectsRequireNonNull_explicitlyNullable_inNullMarkedScope() {
    compilationHelper
        .setArgs("-XepOpt:RedundantNullCheck:CheckRequireNonNull=true")
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NullMarked;",
            "import org.jspecify.annotations.Nullable;",
            "import java.util.Objects;",
            "@NullMarked",
            "class Test {",
            "  void foo(@Nullable String s) {",
            "    Objects.requireNonNull(s); // This is fine",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_objectsRequireNonNull_outsideNullMarkedScope() {
    compilationHelper
        .setArgs("-XepOpt:RedundantNullCheck:CheckRequireNonNull=true")
        .addSourceLines(
            "Test.java",
            "import java.util.Objects;",
            "class Test {",
            "  void foo(String s) {",
            "    Objects.requireNonNull(s); // This is fine",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_nonNullAnnotated_outsideNullMarkedScope() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.NonNull;",
            "class Test {",
            "  void foo(@NonNull String s) {",
            "    // BUG: Diagnostic contains: RedundantNullCheck",
            "    if (s == null) {}",
            "  }",
            "}")
        .doTest();
  }
}
