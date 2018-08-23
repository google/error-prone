/*
 * Copyright 2012 The Error Prone Authors.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author cushon@google.com (Liam Miller-Cushon) */
@RunWith(JUnit4.class)
public class OptionalEqualityTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(OptionalEquality.class, getClass());
  }

  @Test
  public void testPositiveCase_equal() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            "  boolean f(Optional<Integer> a, Optional<Integer> b) {",
            "    // BUG: Diagnostic contains: a.equals(b)",
            "    return a == b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCase_notEqual() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            "  boolean f(Optional<Integer> a, Optional<Integer> b) {",
            "    // BUG: Diagnostic contains: !a.equals(b)",
            "    return a != b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            "  boolean f(Optional<Integer> b) {",
            "    return b == null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void maybeNull() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            "  boolean f(Optional<Integer> a) {",
            "    Optional<Integer> b = Optional.of(42);",
            "    // BUG: Diagnostic contains:",
            "    // Did you mean 'return Objects.equals(a, b);' or 'return a.equals(b);'?",
            "    return a == b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void definitelyNull() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            "  boolean f() {",
            "    Optional<Integer> a = null;",
            "    Optional<Integer> b = Optional.of(42);",
            "    // BUG: Diagnostic contains: Did you mean 'return Objects.equals(a, b);'?",
            "    return a == b;",
            "  }",
            "}")
        .doTest();
  }
}
