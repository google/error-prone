/*
 * Copyright 2019 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link OptionalMapToOptional} bugpattern. */
@RunWith(JUnit4.class)
public final class OptionalMapToOptionalTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(OptionalMapToOptional.class, getClass());

  @Test
  public void positiveWithJavaOptional() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  public boolean test(Optional<Integer> optional) {",
            "    // BUG: Diagnostic contains:",
            "    return optional.map(i -> Optional.of(1)).isPresent();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveWithGuavaOptional() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            "  public boolean test(Optional<Integer> optional) {",
            "    // BUG: Diagnostic contains:",
            "    return optional.transform(i -> Optional.of(1)).isPresent();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveReturned() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            "  public Optional<Optional<Integer>> test(Optional<Integer> optional) {",
            "    // BUG: Diagnostic contains:",
            "    return optional.transform(i -> Optional.of(1));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeFlatMap() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  public Optional<Integer> test(Optional<Integer> optional) {",
            "    return optional.flatMap(i -> Optional.of(1));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeNotToOptional() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  public Optional<Integer> test(Optional<Integer> optional) {",
            "    return optional.map(i -> 1);",
            "  }",
            "}")
        .doTest();
  }
}
