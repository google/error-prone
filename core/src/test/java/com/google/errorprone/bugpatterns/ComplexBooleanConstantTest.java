/*
 * Copyright 2017 The Error Prone Authors.
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
 * Unit tests for {@link ComplexBooleanConstantTest}.
 *
 * @author Sumit Bhagwani (bhagwani@google.com)
 */
@RunWith(JUnit4.class)
public class ComplexBooleanConstantTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new ComplexBooleanConstant(), getClass());

  @Test
  public void refactorTest() {
    refactoringHelper
        .addInputLines(
            "in/Foo.java",
            "class Foo {",
            "  int CONSTANT1 = 1;",
            "  int CONSTANT2 = 1;",
            "  int barNoOp() {",
            "    return 1 - 1;",
            "  }",
            "  boolean barNoOpWithConstants() {",
            "    return CONSTANT1 == CONSTANT2;",
            "  }",
            "  boolean barEquals() {",
            "    return 1 == 1;",
            "  }",
            "  boolean barNotEquals() {",
            "    return 1 != 1;",
            "  }",
            "  boolean f(boolean x) {",
            "    boolean r;",
            "    r = x || !false;",
            "    r = x || !true;",
            "    r = x || true;",
            "    r = x && !false;",
            "    r = x && !true;",
            "    r = x && false;",
            "    return r;",
            "  }",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "class Foo {",
            "  int CONSTANT1 = 1;",
            "  int CONSTANT2 = 1;",
            "  int barNoOp() {",
            "    return 1 - 1;",
            "  }",
            "  boolean barNoOpWithConstants() {",
            "    return CONSTANT1 == CONSTANT2;",
            "  }",
            "  boolean barEquals() {",
            "    return true;",
            "  }",
            "  boolean barNotEquals() {",
            "    return false;",
            "  }",
            "  boolean f(boolean x) {",
            "    boolean r;",
            "    r = true;",
            "    r = x || !true;",
            "    r = true;",
            "    r = x && !false;",
            "    r = false;",
            "    r = false;",
            "    return r;",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void negative() {
    CompilationTestHelper.newInstance(ComplexBooleanConstant.class, getClass())
        .addSourceLines(
            "A.java", //
            "package a;",
            "class A {",
            "  static final int A = 1;",
            "  static final int B = 2;",
            "  static final boolean C = A > B;",
            "  static final boolean D = A + B > 0;",
            "  static final boolean E = (A + B) > 0;",
            "}")
        .doTest();
  }
}
