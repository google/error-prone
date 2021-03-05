/*
 * Copyright 2020 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link NullOptional}. */
@RunWith(JUnit4.class)
public final class NullOptionalTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(NullOptional.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(NullOptional.class, getClass());

  @Test
  public void simplePositiveCase() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  void a(Optional<Object> o) {}",
            "  void test() {",
            "    a(null);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  void a(Optional<Object> o) {}",
            "  void test() {",
            "    a(Optional.empty());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void annotatedWithNullable_noMatch() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import javax.annotation.Nullable;",
            "class Test {",
            "  void a(@Nullable Optional<Object> o) {}",
            "  void test() {",
            "    a(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void notPassingNull_noMatch() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  void a(Optional<Object> o) {}",
            "  void test() {",
            "    a(Optional.empty());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void withinAssertThrows_noMatch() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.junit.Assert.assertThrows;",
            "import java.util.Optional;",
            "class Test {",
            "  void a(Optional<Object> o) {}",
            "  void test() {",
            "    assertThrows(NullPointerException.class, () -> a(null));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lastVarArgsParameter_match() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  @SafeVarargs",
            "  private final void a(int a, Optional<Object>... o) {}",
            "  void test() {",
            "    // BUG: Diagnostic contains:",
            "    a(1, Optional.empty(), null);",
            "  }",
            "}")
        .doTest();
  }
}
