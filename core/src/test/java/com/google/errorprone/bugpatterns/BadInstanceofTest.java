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
 * Tests for {@link BadInstanceof} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class BadInstanceofTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(BadInstanceof.class, getClass());

  @Test
  public void refactoring() {
    BugCheckerRefactoringTestHelper.newInstance(new BadInstanceof(), getClass())
        .addInputLines(
            "Test.java",
            "class A {",
            "  boolean foo(C c) {",
            "    return c instanceof A;",
            "  }",
            "  boolean notFoo(C c) {",
            "    return !(c instanceof A);",
            "  }",
            "  static class C extends A {}",
            "}")
        .addOutputLines(
            "Test.java",
            "class A {",
            "  boolean foo(C c) {",
            "    return c != null;",
            "  }",
            "  boolean notFoo(C c) {",
            "    return c == null;",
            "  }",
            "  static class C extends A {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class A {",
            "  // BUG: Diagnostic contains: `new C()` is a non-null instance of C "
                + "which is a subtype of A",
            "  boolean t = new C() instanceof A;",
            "  boolean foo(C c) {",
            "    // BUG: Diagnostic contains: `c` is an instance of C which is a subtype of A",
            "    return c instanceof A;",
            "  }",
            "  boolean notFoo(C c) {",
            "    // BUG: Diagnostic contains: `c` is an instance of C which is a subtype of A",
            "    return !(c instanceof A);",
            "  }",
            "}",
            "class C extends A {}")
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class A {",
            "  boolean foo(A a) {",
            "    return a instanceof C;",
            "  }",
            "}",
            "class C extends A {}")
        .doTest();
  }
}
