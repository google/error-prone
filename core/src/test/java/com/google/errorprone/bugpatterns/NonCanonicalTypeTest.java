/*
 * Copyright 2019 The Error Prone Authors.
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

/** Tests for {@link NonCanonicalType}. */
@RunWith(JUnit4.class)
public final class NonCanonicalTypeTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(NonCanonicalType.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            "  void test() {",
            "    // BUG: Diagnostic contains: `Map.Entry` was referred to by the"
                + " non-canonical name `ImmutableMap.Entry`",
            "    ImmutableMap.Entry<?, ?> entry = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void differingOnlyByPackageName() {
    compilationHelper
        .addSourceLines(
            "foo/A.java", //
            "package foo;",
            "public class A {",
            "  public static class B {}",
            "}")
        .addSourceLines(
            "bar/A.java", //
            "package bar;",
            "public class A extends foo.A {}")
        .addSourceLines(
            "D.java", //
            "package bar;",
            "import bar.A;",
            "public interface D {",
            "  // BUG: Diagnostic contains: The type `foo.A.B` was referred to by the"
                + " non-canonical name `bar.A.B`",
            "  A.B test();",
            "}")
        .doTest();
  }

  @Test
  public void positiveWithGenerics() {
    compilationHelper
        .addSourceLines(
            "A.java", //
            "class A<T> {",
            "  class B {}",
            "}")
        .addSourceLines(
            "AString.java", //
            "class AString extends A<String> {}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            // TODO(b/116104523): This should be flagged.
            "  AString.B test() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Map;",
            "class Test {",
            "  void test() {",
            "    Map.Entry<?, ?> entry = null;",
            "  }",
            "}")
        .doTest();
  }
}
