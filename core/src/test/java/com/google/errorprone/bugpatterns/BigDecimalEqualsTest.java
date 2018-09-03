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
 * Unit tests for {@link BigDecimalEquals}.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class BigDecimalEqualsTest {
  @Test
  public void positive() {
    CompilationTestHelper.newInstance(BigDecimalEquals.class, getClass())
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Objects;",
            "import java.math.BigDecimal;",
            "class Test {",
            "  void test(BigDecimal a, BigDecimal b) {",
            "    // BUG: Diagnostic contains:",
            "    boolean foo = a.equals(b);",
            "    // BUG: Diagnostic contains:",
            "    boolean bar = !a.equals(b);",
            "    // BUG: Diagnostic contains:",
            "    boolean baz = Objects.equal(a, b);",
            "  }",
            "}")
        .doTest();
  }


  @Test
  public void negative() {
    CompilationTestHelper.newInstance(BigDecimalEquals.class, getClass())
        .addSourceLines(
            "Test.java",
            "import java.math.BigDecimal;",
            "class Test {",
            "  BigDecimal a;",
            "  BigDecimal b;",
            "  boolean f(String a, String b) {",
            "    return a.equals(b);",
            "  }",
            "  @Override public boolean equals(Object o) {",
            "    return a.equals(b);",
            "  }",
            "}")
        .doTest();
  }
}
