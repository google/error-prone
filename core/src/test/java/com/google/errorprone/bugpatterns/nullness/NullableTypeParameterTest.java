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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NullableTypeParameterTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(NullableTypeParameter.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addInputLines(
            "T.java",
            "import java.util.List;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  interface I {}",
            "  interface J {}",
            "  <@Nullable X, @NonNull Y> void f() {}",
            "  <@Nullable X extends Object> void h() {}",
            "  <@Nullable X extends I & J> void i() {}",
            "}")
        .addOutputLines(
            "T.java",
            "import java.util.List;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  interface I {}",
            "  interface J {}",
            "  <X extends @Nullable Object, Y extends @NonNull Object> void f() {}",
            "  <X extends @Nullable Object> void h() {}",
            "  <X extends @Nullable I & @Nullable J> void i() {}",
            "}")
        .doTest();
  }

  @Test
  public void noFix() {
    testHelper
        .addInputLines(
            "T.java",
            "import java.util.List;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  interface I {}",
            "  interface J {}",
            "  <@Nullable @NonNull X> void f() {}",
            "  <@Nullable X extends @Nullable Object> void g() {}",
            "  <@Nullable X extends I & @Nullable J> void h() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void diagnostics() {
    CompilationTestHelper.newInstance(NullableTypeParameter.class, getClass())
        .addSourceLines(
            "T.java",
            "import java.util.List;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  interface I {}",
            "  interface J {}",
            "  // BUG: Diagnostic contains:",
            "  <@Nullable @NonNull X> void f() {}",
            "  // BUG: Diagnostic contains:",
            "  <@Nullable X extends @Nullable Object> void g() {}",
            "  // BUG: Diagnostic contains:",
            "  <@Nullable X extends I & @Nullable J> void h() {}",
            "}")
        .doTest();
  }
}
