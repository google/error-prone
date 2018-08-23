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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link EqualsUnsafeCast} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class EqualsUnsafeCastTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(EqualsUnsafeCast.class, getClass());

  @Test
  public void fixes() {
    BugCheckerRefactoringTestHelper.newInstance(new EqualsUnsafeCast(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    Test that = (Test) o;",
            "    return that.a == a;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    if (!(o instanceof Test)) { return false; }",
            "    Test that = (Test) o;",
            "    return that.a == a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fixesInlineCheck() {
    BugCheckerRefactoringTestHelper.newInstance(new EqualsUnsafeCast(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    return o != null && a == ((Test) o).a;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    if (!(o instanceof Test)) { return false; }",
            "    return o != null && a == ((Test) o).a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveWrongType() {
    helper
        .addSourceLines(
            "Test.java",
            "class SubTest extends Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    if (!(o instanceof Test)) { return false; }",
            "    // BUG: Diagnostic contains: instanceof SubTest",
            "    return o != null && a == ((SubTest) o).a;",
            "  }",
            "}",
            "class Test {}")
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    if (getClass() == o.getClass()) { return false; }",
            "    return o != null && a == ((Test) o).a;",
            "  }",
            "}")
        .doTest();

    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    if (!(o instanceof Test)) { return false; }",
            "    return o != null && a == ((Test) o).a;",
            "  }",
            "}")
        .doTest();
  }
}
