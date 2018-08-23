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
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link EqualsGetClass} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class EqualsGetClassTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(EqualsGetClass.class, getClass());

  @Test
  public void fixes_inline() {
    BugCheckerRefactoringTestHelper.newInstance(new EqualsGetClass(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    return o != null && o.getClass().equals(getClass()) && a == ((Test) o).a;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    return o instanceof Test && a == ((Test) o).a;",
            "  }",
            "}")
        .doTest(TestMode.AST_MATCH);
  }

  @Test
  public void fixes_extraParens() {
    BugCheckerRefactoringTestHelper.newInstance(new EqualsGetClass(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    return (o != null) && (o.getClass() == getClass()) && a == ((Test) o).a;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    return (o instanceof Test) && a == ((Test) o).a;",
            "  }",
            "}")
        .doTest(TestMode.AST_MATCH);
  }

  @Test
  public void fixes_separateNullCheck() {
    BugCheckerRefactoringTestHelper.newInstance(new EqualsGetClass(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    if (o == null) { return false; }",
            "    if (o.getClass() != getClass()) { return false; }",
            "    return ((Test) o).a == a;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    if (!(o instanceof Test)) { return false; }",
            "    return ((Test) o).a == a;",
            "  }",
            "}")
        .doTest(TestMode.AST_MATCH);

    BugCheckerRefactoringTestHelper.newInstance(new EqualsGetClass(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    if (o == null) {",
            "      return false;",
            "    } else {",
            "      if (o.getClass() != getClass()) { return false; }",
            "      return ((Test) o).a == a;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    if (!(o instanceof Test)) { return false; }",
            "    return ((Test) o).a == a;",
            "  }",
            "}")
        .doTest(TestMode.AST_MATCH);

    BugCheckerRefactoringTestHelper.newInstance(new EqualsGetClass(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    if (o == null)",
            "      return false;",
            "    else",
            "      return o.getClass() == getClass() && ((Test) o).a == a;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    return o instanceof Test && ((Test) o).a == a;",
            "  }",
            "}")
        .doTest(TestMode.AST_MATCH);
  }

  @Test
  public void positive_unfixable() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Objects;",
            "class Test {",
            "  private int a;",
            "  // BUG: Diagnostic matches: NO_FIX",
            "  @Override public boolean equals(Object o) {",
            "    if (o == null) { return false; }",
            "    if (!Objects.equal(o.getClass(), getClass())) { return false; }",
            "    return ((Test) o).a == a;",
            "  }",
            "}")
        .expectErrorMessage("NO_FIX", fix -> !fix.contains("instanceof Test"))
        .doTest();
  }

  @Test
  public void negative_final() {
    helper
        .addSourceLines(
            "Test.java",
            "final class Test {",
            "  private int a;",
            "  @Override public boolean equals(Object o) {",
            "    if (o == null) { return false; }",
            "    if (o.getClass() != getClass()) { return false; }",
            "    return ((Test) o).a == a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_anonymous() {
    helper
        .addSourceLines(
            "Test.java",
            "final class Test {",
            "  Object foo = new Object() {",
            "    @Override public boolean equals(Object o) {",
            "      if (o == null) { return false; }",
            "      return o.getClass() == getClass();",
            "    }",
            "  };",
            "}")
        .doTest();
  }

  @Test
  public void negative_notOnParameter() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private Object a;",
            "  @Override public boolean equals(Object o) {",
            "    if (o == null) { return false; }",
            "    if (!(o instanceof Test)) { return false; }",
            "    return ((Test) o).a.getClass() == a.getClass();",
            "  }",
            "}")
        .doTest();
  }
}
