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

import static org.junit.Assume.assumeTrue;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link VoidMissingNullable}Test */
@RunWith(JUnit4.class)
public class VoidMissingNullableTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(VoidMissingNullable.class, getClass());

  @Test
  public void positive() {
    compilationHelper
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
  public void negativeAlreadyAnnotated() {
    compilationHelper
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
    compilationHelper
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
    compilationHelper
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
    compilationHelper
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
    compilationHelper
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
  public void negativeTypeArgumentNotVoid() {
    compilationHelper
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
  public void positiveLambdaParameter() {
    compilationHelper
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
    compilationHelper
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
    assumeTrue(RuntimeVersion.isAtLeast10());
    compilationHelper
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
    compilationHelper
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
}
