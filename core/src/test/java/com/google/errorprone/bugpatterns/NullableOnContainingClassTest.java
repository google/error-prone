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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link NullableOnContainingClass}. */
@RunWith(JUnit4.class)
public final class NullableOnContainingClassTest {
  public final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(NullableOnContainingClass.class, getClass());

  @Test
  public void annotationNotNamedNullable_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import static java.lang.annotation.ElementType.TYPE_USE;",
            "import java.lang.annotation.Target;",
            "class A {",
            "  @Target(TYPE_USE)",
            "  @interface Anno {}",
            "  class B {}",
            "  void test(@Anno A.B x) {}",
            "  void test2(A.@Anno B x) {}",
            "}")
        .doTest();
  }

  @Test
  public void annotationNamedNullable_annotatingOuterClass() {
    helper
        .addSourceLines(
            "Test.java",
            "import static java.lang.annotation.ElementType.TYPE_USE;",
            "import java.lang.annotation.Target;",
            "import java.util.List;",
            "class A {",
            "  @Target(TYPE_USE)",
            "  @interface Nullable {}",
            "  class B {}",
            "  // BUG: Diagnostic contains: A.@Nullable B",
            "  void test(@Nullable A.B x) {}",
            "  // BUG: Diagnostic contains: List< A.@Nullable B>",
            "  void test2(List<@Nullable A.B> x) {}",
            "}")
        .doTest();
  }

  @Test
  public void annotationNamedNullable_annotatingInImplements() {
    helper
        .addSourceLines(
            "Test.java",
            "import static java.lang.annotation.ElementType.TYPE_USE;",
            "import java.lang.annotation.Target;",
            "import java.util.List;",
            "interface A {",
            "  @Target(TYPE_USE)",
            "  @interface Nullable {}",
            "  // BUG: Diagnostic contains: A.@Nullable B",
            "  abstract class B implements List<@Nullable A.B> {}",
            "}")
        .doTest();
  }

  @Test
  public void annotationNamedNullable_annotatingInnerClass() {
    helper
        .addSourceLines(
            "Test.java",
            "import static java.lang.annotation.ElementType.TYPE_USE;",
            "import java.lang.annotation.Target;",
            "class A {",
            "  @Target(TYPE_USE)",
            "  @interface Nullable {}",
            "  class B {}",
            "  void test(A.@Nullable B x) {}",
            "}")
        .doTest();
  }
}
