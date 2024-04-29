/*
 * Copyright 2022 The Error Prone Authors.
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

/** {@link NullArgumentForNonNullParameter}Test */
@RunWith(JUnit4.class)
public class NullArgumentForNonNullParameterTest {
  private final CompilationTestHelper conservativeHelper =
      CompilationTestHelper.newInstance(NullArgumentForNonNullParameter.class, getClass());
  private final CompilationTestHelper aggressiveHelper =
      CompilationTestHelper.newInstance(NullArgumentForNonNullParameter.class, getClass())
          .setArgs("-XepOpt:Nullness:Conservative=false");
  private final BugCheckerRefactoringTestHelper aggressiveRefactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(NullArgumentForNonNullParameter.class, getClass())
          .setArgs("-XepOpt:Nullness:Conservative=false");

  @Test
  public void positivePrimitive() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "import java.util.Optional;",
            "class Foo {",
            "  void consume(int i) {}",
            "  void foo(Optional<Integer> o) {",
            "    // BUG: Diagnostic contains: ",
            "    consume(o.orElse(null));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveAnnotatedNonnullAggressive() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "import javax.annotation.Nonnull;",
            "class Foo {",
            "  void consume(@Nonnull String s) {}",
            "  void foo() {",
            "    // BUG: Diagnostic contains: ",
            "    consume(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeAnnotatedNonnullConservative() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "import javax.annotation.Nonnull;",
            "class Foo {",
            "  void consume(@Nonnull String s) {}",
            "  void foo() {",
            "    consume(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveJavaOptionalOf() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "import java.util.Optional;",
            "class Foo {",
            "  void foo() {",
            "    // BUG: Diagnostic contains: ",
            "    Optional.of(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveGuavaOptionalOf() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "import com.google.common.base.Optional;",
            "class Foo {",
            "  void foo() {",
            "    // BUG: Diagnostic contains: ",
            "    Optional.of(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveGuavaImmutableSetOf() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "import com.google.common.collect.ImmutableSet;",
            "class Foo {",
            "  void foo() {",
            "    // BUG: Diagnostic contains: ",
            "    ImmutableSet.of(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveGuavaImmutableSetBuilderAdd() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "import com.google.common.collect.ImmutableSet;",
            "class Foo {",
            "  void foo(boolean b) {",
            "    // BUG: Diagnostic contains: ",
            // We use a ternary to avoid:
            // "non-varargs call of varargs method with inexact argument type for last parameter"
            "    ImmutableSet.builder().add(b ? 1 : null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveArgumentCaptorForClass() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "import org.mockito.ArgumentCaptor;",
            "class Foo {",
            "  void foo() {",
            "    // BUG: Diagnostic contains: ",
            "    ArgumentCaptor.forClass(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeNullMarkedComGoogleCommonButNullable() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "import com.google.common.collect.ImmutableSet;",
            "class Foo {",
            "  void foo() {",
            "    ImmutableSet.of().contains(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveNullMarkedOtherPackageAggressive() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Foo {",
            "  void consume(String s) {}",
            "  void foo() {",
            "    // BUG: Diagnostic contains: ",
            "    consume(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeNullMarkedNonComGoogleCommonPackageConservative() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "import org.jspecify.annotations.NullMarked;",
            "@NullMarked",
            "class Foo {",
            "  void consume(String s) {}",
            "  void foo() {",
            "    consume(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeNullMarkedTypeVariable() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "import com.google.common.collect.ConcurrentHashMultiset;",
            "class Foo {",
            "  void foo() {",
            "    ConcurrentHashMultiset.create().add(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativePrimitiveButEnclosingClass() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "import java.util.Optional;",
            "class Foo {",
            "  class Bar {",
            "    Bar(int i) {}",
            "  }",
            "  void foo(Optional<Integer> o) {",
            // It would be nice to catch this, but for now, at least, we don't.
            "    new Bar(o.orElse(null));",
            "  }",
            "}")
        .doTest();
  }
}
