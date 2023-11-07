/*
 * Copyright 2023 The Error Prone Authors.
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
public class MultipleNullnessAnnotationsTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(MultipleNullnessAnnotations.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import org.checkerframework.checker.nullness.compatqual.NonNullDecl;",
            "import org.checkerframework.checker.nullness.compatqual.NullableDecl;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import java.util.List;",
            "abstract class Test {",
            "  // BUG: Diagnostic contains:",
            "  @Nullable @NonNull Object x;",
            "  // BUG: Diagnostic contains:",
            "  @NullableDecl static @NonNull Object y;",
            "  // BUG: Diagnostic contains:",
            "  List<@Nullable @NonNull String> z;",
            "  // BUG: Diagnostic contains:",
            "  @NullableDecl abstract @NonNull Object f();",
            "  // BUG: Diagnostic contains:",
            "  abstract void f(@NullableDecl Object @NonNull[] x);",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import org.checkerframework.checker.nullness.compatqual.NonNullDecl;",
            "import org.checkerframework.checker.nullness.compatqual.NullableDecl;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import java.util.List;",
            "abstract class Test {",
            "  @NonNullDecl @NonNull Object x;",
            "  @NullableDecl static @Nullable Object y;",
            "}")
        .doTest();
  }

  @Test
  public void disambiguation() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import java.util.List;",
            "abstract class Test {",
            "  @Nullable Object @Nullable [] x;",
            "  abstract void f(@Nullable Object @Nullable ... x);",
            "}")
        .doTest();
  }

  @Test
  public void declarationAndType() {
    testHelper
        .addSourceLines(
            "Nullable.java",
            "import java.lang.annotation.Target;",
            "import java.lang.annotation.ElementType;",
            "@Target({",
            "  ElementType.METHOD,",
            "  ElementType.FIELD,",
            "  ElementType.PARAMETER,",
            "  ElementType.LOCAL_VARIABLE,",
            "  ElementType.TYPE_USE",
            "})",
            "public @interface Nullable {}")
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void f(@Nullable Object x);",
            "  abstract @Nullable Object g();",
            "  @Nullable Object f;",
            "}")
        .doTest();
  }
}
