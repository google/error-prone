/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link VoidMissingNullable}Test */
@RunWith(JUnit4.class)
public class VoidMissingNullableTest {
  private final CompilationTestHelper conservativeCompilationHelper =
      CompilationTestHelper.newInstance(VoidMissingNullable.class, getClass());
  private final CompilationTestHelper aggressiveCompilationHelper =
      CompilationTestHelper.newInstance(VoidMissingNullable.class, getClass())
          .setArgs("-XepOpt:Nullness:Conservative=false");
  private final BugCheckerRefactoringTestHelper aggressiveRefactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(VoidMissingNullable.class, getClass())
          .setArgs("-XepOpt:Nullness:Conservative=false");

  @Test
  public void positive() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.Nullable;",
            "class Test {",
            "  // BUG: Diagnostic contains: @Nullable",
            "  Void v;",
            "  // BUG: Diagnostic contains: @Nullable",
            "  Void f() {",
            "    return v;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDeclarationAnnotatedLocation() {
    aggressiveRefactoringHelper
        .addInputLines(
            "in/Foo.java",
            "import javax.annotation.Nullable;",
            "abstract class Foo {",
            "  java.lang.Void v;",
            "  final Void f() {",
            "    return v;",
            "  }",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import javax.annotation.Nullable;",
            "abstract class Foo {",
            "  @Nullable java.lang.Void v;",
            "  @Nullable final Void f() {",
            "    return v;",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void testTypeAnnotatedLocation() {
    aggressiveRefactoringHelper
        .addInputLines(
            "in/Foo.java",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "abstract class Foo {",
            "  java.lang.Void v;",
            "  final Void f() {",
            "    return v;",
            "  }",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "abstract class Foo {",
            "  java.lang.@Nullable Void v;",
            "  final @Nullable Void f() {",
            "    return v;",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negativeAlreadyAnnotated() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.Nullable;",
            "class Test {",
            "  @Nullable Void v;",
            "  @Nullable Void f() {",
            "    return v;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeNotVoid() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.Nullable;",
            "class Test {",
            "  String s;",
            "  String f() {",
            "    return s;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveTypeArgument() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class Test {",
            "  // BUG: Diagnostic contains: @Nullable",
            "  List<Void> a;",
            "  // BUG: Diagnostic contains: @Nullable",
            "  List<? extends Void> b;",
            "  // BUG: Diagnostic contains: @Nullable",
            "  List<? super Void> c;",
            "  List<?> d;",
            "}")
        .doTest();
  }

  @Test
  public void positiveTypeArgumentOtherAnnotation() {
    aggressiveCompilationHelper
        .addSourceLines(
            "NonNull.java",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "import java.lang.annotation.Target;",
            "@Retention(RetentionPolicy.RUNTIME)",
            "@Target(ElementType.TYPE_USE)",
            "public @interface NonNull {}")
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class Test {",
            "  // BUG: Diagnostic contains: @Nullable",
            "  List<@NonNull Void> a;",
            "  // BUG: Diagnostic contains: @Nullable",
            "  List<? extends @NonNull Void> b;",
            "  // BUG: Diagnostic contains: @Nullable",
            "  List<? super @NonNull Void> c;",
            "  List<?> d;",
            "}")
        .doTest();
  }

  @Test
  public void negativeTypeArgumentAlreadyAnnotated() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Nullable.java",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "import java.lang.annotation.Target;",
            "@Retention(RetentionPolicy.RUNTIME)",
            "@Target(ElementType.TYPE_USE)",
            "public @interface Nullable {}")
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test {",
            "  List<@Nullable Void> a;",
            "  List<? extends @Nullable Void> b;",
            "  List<? super @Nullable Void> c;",
            "  List<?> d;",
            "}")
        .doTest();
  }

  @Test
  public void negativeTypeArgumentAlreadyAnnotatedAnonymous() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Nullable.java",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "import java.lang.annotation.Target;",
            "@Retention(RetentionPolicy.RUNTIME)",
            "@Target(ElementType.TYPE_USE)",
            "public @interface Nullable {}")
        .addSourceLines("Bystander.java", "public interface Bystander<T> {}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static {",
            "    new Bystander<@Nullable Void>() {};",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeTypeArgumentNotVoid() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Nullable.java",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "import java.lang.annotation.Target;",
            "@Retention(RetentionPolicy.RUNTIME)",
            "@Target(ElementType.TYPE_USE)",
            "public @interface Nullable {}")
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test {",
            "  List<String> a;",
            "  List<? extends String> b;",
            "  List<? super String> c;",
            "  List<?> d;",
            "}")
        .doTest();
  }

  @Test
  public void negativeTypeArgumentDeclarationNullable() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "import javax.annotation.Nullable;",
            "class Test {",
            "  List<Void> a;",
            "  List<? extends Void> b;",
            "  List<? super Void> c;",
            "  List<?> d;",
            "}")
        .doTest();
  }

  @Test
  public void positiveLambdaParameter() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.Nullable;",
            "interface Test {",
            "  void consume(@Nullable Void v);",
            "",
            "  // BUG: Diagnostic contains: @Nullable",
            "  Test TEST = (Void v) -> {};",
            "}")
        .doTest();
  }

  @Test
  public void negativeLambdaParameterNoType() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.Nullable;",
            "interface Test {",
            "  void consume(@Nullable Void v);",
            "",
            "  Test TEST = v -> {};",
            "}")
        .doTest();
  }

  // TODO(cpovirk): Test under Java 11+ with `(var x) -> {}` lambda syntax.

  @Test
  public void negativeVar() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.Nullable;",
            "class Test {",
            "  @Nullable Void v;",
            "",
            "  void f() {",
            "    var v = this.v;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeOtherLocalVariable() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.Nullable;",
            "class Test {",
            "  @Nullable Void v;",
            "",
            "  void f() {",
            "    Void v = this.v;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveConservativeNullMarked() {
    conservativeCompilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.Nullable;",
            "import org.jspecify.nullness.NullMarked;",
            "@NullMarked",
            "class Test {",
            "  // BUG: Diagnostic contains: @Nullable",
            "  Void v;",
            "}")
        .doTest();
  }

  @Test
  public void negativeConservativeNotNullMarked() {
    conservativeCompilationHelper
        .addSourceLines(
            "Test.java", //
            "import javax.annotation.Nullable;",
            "class Test {",
            "  Void v;",
            "}")
        .doTest();
  }
}
