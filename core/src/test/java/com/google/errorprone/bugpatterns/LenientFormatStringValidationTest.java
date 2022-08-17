/*
 * Copyright 2022 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public final class LenientFormatStringValidationTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(LenientFormatStringValidation.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(LenientFormatStringValidation.class, getClass());

  @Test
  public void tooFewArguments() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "class Test {",
            "  void test() {",
            "    // BUG: Diagnostic contains:",
            "    Preconditions.checkState(false, \"%s\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void tooManyArguments() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "class Test {",
            "  void test() {",
            "    // BUG: Diagnostic contains:",
            "    Preconditions.checkState(false, \"%s\", 1, 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void tooManyArguments_fix() {
    refactoring
        .addInputLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "class Test {",
            "  void test() {",
            "    Preconditions.checkState(false, \"%s\", 1, 1);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "class Test {",
            "  void test() {",
            "    Preconditions.checkState(false, \"%s (%s)\", 1, 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void tooManyArguments_fixWithNonLiteral() {
    refactoring
        .addInputLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "class Test {",
            "  private static final String FOO = \"%s\";",
            "  void test() {",
            "    Preconditions.checkState(false, FOO, 1, 1);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "class Test {",
            "  private static final String FOO = \"%s\";",
            "  void test() {",
            "    Preconditions.checkState(false, FOO + \" (%s)\", 1, 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void correctArguments() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "class Test {",
            "  void test() {",
            "    Preconditions.checkState(false, \"%s\", 1);",
            "  }",
            "}")
        .doTest();
  }
}
