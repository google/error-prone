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
 * Unit tests for {@link AnnotateFormatMethod} bug pattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class AnnotateFormatMethodTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(AnnotateFormatMethod.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "AnnotateFormatMethodPositiveCases.java",
            "class AnnotateFormatMethodPositiveCases {",
            "  // BUG: Diagnostic contains: FormatMethod",
            "  String formatMe(String formatString, Object... args) {",
            "    return String.format(formatString, args);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    BugCheckerRefactoringTestHelper.newInstance(new AnnotateFormatMethod(), getClass())
        .addInputLines(
            "AnnotateFormatMethodPositiveCases.java",
            "class AnnotateFormatMethodPositiveCases {",
            "  String formatMe(String formatString, Object... args) {",
            "    return String.format(formatString, args);",
            "  }",
            "}")
        .addOutputLines(
            "AnnotateFormatMethodPositiveCases_expected.java",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "class AnnotateFormatMethodPositiveCases {",
            "  @FormatMethod",
            "  String formatMe(@FormatString String formatString, Object... args) {",
            "    return String.format(formatString, args);",
            "  }",
            "}")
        .doTest(TestMode.AST_MATCH);
  }

  @Test
  public void alreadyAnnotated() {
    compilationHelper
        .addSourceLines(
            "AnnotateFormatMethodNegativeCases.java",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "class AnnotateFormatMethodNegativeCases {",
            "  @FormatMethod",
            "  String formatMe(@FormatString String formatString, Object... args) {",
            "    return String.format(formatString, args);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void notTerminalArguments() {
    compilationHelper
        .addSourceLines(
            "AnnotateFormatMethodNegativeCases.java",
            "class AnnotateFormatMethodNegativeCases {",
            "  // BUG: Diagnostic contains: reordered",
            "  String formatMe(String formatString, String surprise, Object... args) {",
            "    return String.format(formatString, args);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void notVarArgs() {
    compilationHelper
        .addSourceLines(
            "AnnotateFormatMethodNegativeCases.java",
            "class AnnotateFormatMethodNegativeCases {",
            "  String formatMe(String formatString, String justTheOneArgument) {",
            "    return String.format(formatString, justTheOneArgument);",
            "  }",
            "}")
        .doTest();
  }
}
