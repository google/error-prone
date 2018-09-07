/*
 * Copyright 2018 The Error Prone Authors.
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

/**
 * Tests for {@link EqualsUsingHashCode} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class EqualsUsingHashCodeTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(EqualsUsingHashCode.class, getClass());

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Objects;",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    Test that = (Test) o;",
            "    return o.hashCode() == hashCode() && a == that.a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  private int b;",
            "  @Override public boolean equals(Object o) {",
            "    // BUG: Diagnostic contains:",
            "    return o.hashCode() == hashCode();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveBinary() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  private int b;",
            "  @Override public boolean equals(Object o) {",
            "    // BUG: Diagnostic contains:",
            "    return o instanceof Test && o.hashCode() == hashCode();",
            "  }",
            "}")
        .doTest();
  }
}
