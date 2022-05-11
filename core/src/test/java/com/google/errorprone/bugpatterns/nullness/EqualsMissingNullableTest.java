/*
 * Copyright 2021 The Error Prone Authors.
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

/** {@link EqualsMissingNullable}Test */
@RunWith(JUnit4.class)
public class EqualsMissingNullableTest {
  private final CompilationTestHelper conservativeHelper =
      CompilationTestHelper.newInstance(EqualsMissingNullable.class, getClass());
  private final CompilationTestHelper aggressiveHelper =
      CompilationTestHelper.newInstance(EqualsMissingNullable.class, getClass())
          .setArgs("-XepOpt:Nullness:Conservative=false");
  private final BugCheckerRefactoringTestHelper aggressiveRefactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(EqualsMissingNullable.class, getClass())
          .setArgs("-XepOpt:Nullness:Conservative=false");

  @Test
  public void testPositive() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "abstract class Foo {",
            "  // BUG: Diagnostic contains: @Nullable",
            "  public abstract boolean equals(Object o);",
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
            "  public abstract boolean equals(final Object o);",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import javax.annotation.Nullable;",
            "abstract class Foo {",
            "  public abstract boolean equals(@Nullable final Object o);",
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
            "  public abstract boolean equals(final Object o);",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "abstract class Foo {",
            "  public abstract boolean equals(final @Nullable Object o);",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void testNegativeAlreadyAnnotated() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "import javax.annotation.Nullable;",
            "abstract class Foo {",
            "  public abstract boolean equals(@Nullable Object o);",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeAlreadyAnnotatedWithProtobufAnnotation() {
    aggressiveHelper
        .addSourceLines(
            "ProtoMethodAcceptsNullParameter.java", "@interface ProtoMethodAcceptsNullParameter {}")
        .addSourceLines(
            "Foo.java",
            "abstract class Foo {",
            "  public abstract boolean equals(@ProtoMethodAcceptsNullParameter Object o);",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeNotObjectEquals() {
    aggressiveHelper
        .addSourceLines(
            "Foo.java",
            "abstract class Foo {",
            "  public abstract boolean equals(String s, int i);",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveConservativeNullMarked() {
    conservativeHelper
        .addSourceLines(
            "Foo.java",
            "import org.jspecify.nullness.NullMarked;",
            "@NullMarked",
            "abstract class Foo {",
            "  // BUG: Diagnostic contains: @Nullable",
            "  public abstract boolean equals(Object o);",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeConservativeNotNullMarked() {
    conservativeHelper
        .addSourceLines(
            "Foo.java", //
            "abstract class Foo {",
            "  public abstract boolean equals(Object o);",
            "}")
        .doTest();
  }
}
