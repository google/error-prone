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

/** Tests for {@link CheckNotNullMultipleTimes}. */
@RunWith(JUnit4.class)
public final class CheckNotNullMultipleTimesTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(CheckNotNullMultipleTimes.class, getClass());

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "class Test {",
            "  Test(Integer a, Integer b) {",
            "    checkNotNull(a);",
            "    // BUG: Diagnostic contains:",
            "    checkNotNull(a);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "class Test {",
            "  Test(Integer a, Integer b) {",
            "    checkNotNull(a);",
            "    checkNotNull(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void conditional_noError() {
    helper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "class Test {",
            "  Test(Integer a) {",
            "    if (hashCode() > 0) {",
            "      checkNotNull(a);",
            "    } else {",
            "      checkNotNull(a);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assignedTwice_noError() {
    helper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "class Test {",
            "  int a;",
            "  Test(Integer a) {",
            "    this.a = checkNotNull(a);",
            "    checkNotNull(a);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void withinTryAndCatch_noError() {
    helper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "class Test {",
            "  Test(Integer a) {",
            "    try {",
            // There could be intervening lines here which would throw, leading to the first check
            // not being reached.
            "      checkNotNull(a);",
            "    } catch (Exception e) {",
            "      checkNotNull(a);",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
