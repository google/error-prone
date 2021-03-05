/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.formatstring;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link InlineFormatString}Test */
@RunWith(JUnit4.class)
public class InlineFormatStringTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(InlineFormatString.class, getClass());

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(InlineFormatString.class, getClass());

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private static final String FORMAT = \"hello %s\";",
            "  void f() {",
            "    System.err.printf(FORMAT, 42);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    System.err.printf(\"hello %s\", 42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_multiple() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private static final String FORMAT = \"hello %s\";",
            "  void f() {",
            "    System.err.printf(FORMAT, 42);",
            "    System.err.printf(FORMAT, 43);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_otherUsers() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private static final String FORMAT = \"hello %s\";",
            "  void f() {",
            "    System.err.printf(FORMAT, 42);",
            "    System.err.println(FORMAT);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoring_precondition() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "class Test {",
            "  private static final String FORMAT = \"hello %s\";",
            "  void f(boolean b) {",
            "    Preconditions.checkArgument(b, FORMAT, 42);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "class Test {",
            "  void f(boolean b) {",
            "    Preconditions.checkArgument(b, \"hello %s\", 42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoring_formatMethod() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "abstract class Test {",
            "  @FormatMethod abstract String f(String f, Object... args);",
            "  @FormatMethod abstract String g(boolean b, @FormatString String f, Object... args);",
            "  private static final String FORMAT = \"hello %s\";",
            "  private static final String FORMAT2 = \"hello %s\";",
            "  void h(boolean b) {",
            "    f(FORMAT, 42);",
            "    g(b, FORMAT2, 42);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "abstract class Test {",
            "  @FormatMethod abstract String f(String f, Object... args);",
            "  @FormatMethod abstract String g(boolean b, @FormatString String f, Object... args);",
            "  void h(boolean b) {",
            "    f(\"hello %s\", 42);",
            "    g(b, \"hello %s\", 42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppression() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @SuppressWarnings(\"InlineFormatString\")",
            "  private static final String FORMAT = \"hello %s\";",
            "  void f() {",
            "    System.err.printf(FORMAT, 42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressionClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "@SuppressWarnings(\"InlineFormatString\")",
            "class Test {",
            "  private static final String FORMAT = \"hello %s\";",
            "  void f() {",
            "    System.err.printf(FORMAT, 42);",
            "  }",
            "}")
        .doTest();
  }
}
