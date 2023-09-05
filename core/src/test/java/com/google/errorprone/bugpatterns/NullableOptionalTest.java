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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class NullableOptionalTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(NullableOptional.class, getClass());

  @Test
  public void optionalFieldWithNullableAnnotation_showsError() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import javax.annotation.Nullable;",
            "final class Test {",
            " // BUG: Diagnostic contains:",
            " @Nullable private Optional<Object> foo;",
            "}")
        .doTest();
  }

  @Test
  public void guavaOptionalFieldWithNullableAnnotation_showsError() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "import javax.annotation.Nullable;",
            "final class Test {",
            " @Nullable",
            " // BUG: Diagnostic contains:",
            " private Optional<Object> foo;",
            "}")
        .doTest();
  }

  @Test
  public void methodReturnsOptionalWithNullableAnnotation_showsError() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import javax.annotation.Nullable;",
            "final class Test {",
            " @Nullable",
            " // BUG: Diagnostic contains:",
            " private Optional<Object> foo() {",
            "   return Optional.empty();",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void methodReturnsOptionalWithAnotherNullableAnnotation_showsError() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import org.jspecify.annotations.Nullable;",
            "final class Test {",
            " @Nullable",
            " // BUG: Diagnostic contains:",
            " private Optional<Object> foo() {",
            "   return Optional.empty();",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void methodHasNullableOptionalAsParameter_showsError() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import javax.annotation.Nullable;",
            "final class Test {",
            " // BUG: Diagnostic contains:",
            " private void foo(@Nullable Optional<Object> optional) {}",
            "}")
        .doTest();
  }

  @Test
  public void objectFieldWithNullableAnnotation_noError() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.Nullable;",
            "final class Test {",
            " @Nullable Object field;",
            "}")
        .doTest();
  }

  @Test
  public void methodReturnsNonOptionalWithNullableAnnotation_noError() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.Nullable;",
            "final class Test {",
            " @Nullable",
            " private Object foo() {",
            "   return null;",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void methodReturnsNonOptionalWithAnotherNullableAnnotation_noError() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.Nullable;",
            "final class Test {",
            " @Nullable",
            " private Object foo() {",
            "   return null;",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void methodHasNullableNonOptionalAsParameter_noError() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.jspecify.annotations.Nullable;",
            "final class Test {",
            " private void foo(@Nullable Object object) {}",
            "}")
        .doTest();
  }
}
