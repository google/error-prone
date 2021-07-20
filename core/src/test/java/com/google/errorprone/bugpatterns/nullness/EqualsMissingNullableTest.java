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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link EqualsMissingNullable}Test */
@RunWith(JUnit4.class)
public class EqualsMissingNullableTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(EqualsMissingNullable.class, getClass());

  @Test
  public void testPositive() {
    helper
        .addSourceLines(
            "Foo.java",
            "abstract class Foo {",
            "  // BUG: Diagnostic contains: @Nullable",
            "  public abstract boolean equals(Object o);",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeAlreadyAnnotated() {
    helper
        .addSourceLines(
            "Foo.java",
            "import javax.annotation.Nullable;",
            "abstract class Foo {",
            "  public abstract boolean equals(@Nullable Object o);",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeNotObjectEquals() {
    helper
        .addSourceLines(
            "Foo.java",
            "abstract class Foo {",
            "  public abstract boolean equals(String s, int i);",
            "}")
        .doTest();
  }
}
