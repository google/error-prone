/*
 * Copyright 2023 The Error Prone Authors.
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

import static org.junit.Assume.assumeTrue;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class StringFormatWithLiteralTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(StringFormatWithLiteral.class, getClass());

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(StringFormatWithLiteral.class, getClass());

  @Test
  public void negativeStringFormatWithNonTrivialHexFormattingLiteral() {
    compilationHelper
        .addSourceLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return String.format(\"This number %02x will be formatted\", 101);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeStringFormatWithNonTrivialFloatFormattingLiteral() {
    compilationHelper
        .addSourceLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return String.format(\"Formatting this float: %f\", 101.0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeStringFormatWithVariableAsFormatString() {
    compilationHelper
        .addSourceLines(
            "ExampleClass.java",
            "import java.lang.String;",
            "public class ExampleClass {",
            "  String test() {",
            "    String formatString = \"Formatting this string: %s\";",
            "    return String.format(formatString, \"data\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeStringFormatWithVariableAsFormatStringAndNoArguments() {
    compilationHelper
        .addSourceLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    String formatString = \"Nothing to format\";",
            "    return String.format(formatString);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeStringFormatWithNewLine() {
    compilationHelper
        .addSourceLines(
            "ExampleClass.java",
            "import java.lang.String;",
            "public class ExampleClass {",
            "  String test() {",
            "    return String.format(\"%n\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeStringFormatWithOneStringVariable() {
    compilationHelper
        .addSourceLines(
            "ExampleClass.java",
            "import java.lang.String;",
            "public class ExampleClass {",
            "  String test() {",
            "    String data = \"data\";",
            "    return String.format(\"Formatting this string: %s\", data);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeStringFormatWithOneIntegerVariable() {
    compilationHelper
        .addSourceLines(
            "ExampleClass.java",
            "import java.lang.String;",
            "public class ExampleClass {",
            "  String test() {",
            "    Integer data = 3;",
            "    return String.format(\"Formatting this int: %d\", data);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeStringFormatWithOneIntegerVariableAndStringLiteral() {
    compilationHelper
        .addSourceLines(
            "ExampleClass.java",
            "import java.lang.String;",
            "public class ExampleClass {",
            "  String test() {",
            "    Integer data = 3;",
            "    return String.format(\"Formatting this int: %d;Formatting this string: %s\",",
            "                         data, \"string\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeStringFormatWithOneStringVariableStaticImport() {
    compilationHelper
        .addSourceLines(
            "ExampleClass.java",
            "import static java.lang.String.format;",
            "public class ExampleClass {",
            "  String test() {",
            "    String data = \"data\";",
            "    return format(\"Formatting this string: %s\", data);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeOtherMethod() {
    compilationHelper
        .addSourceLines(
            "ExampleClass.java",
            "import java.lang.String;",
            "public class ExampleClass {",
            "  String test() {",
            "    return String.valueOf(true);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringStringFormatWithNoArguments() {
    refactoringHelper
        .addInputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return String.format(\"Formatting nothing\");",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return \"Formatting nothing\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringFormattedWithNoArguments() {
    assumeTrue(RuntimeVersion.isAtLeast15());
    refactoringHelper
        .addInputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return \"Formatting nothing\".formatted();",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return \"Formatting nothing\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringStringFormatWithIntegerLiteral() {
    refactoringHelper
        .addInputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return String.format(\"Formatting this integer: %d\", 1);",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return \"Formatting this integer: 1\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringFormattedWithIntegerLiteral() {
    assumeTrue(RuntimeVersion.isAtLeast15());
    refactoringHelper
        .addInputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return \"Formatting this integer: %d\".formatted(1);",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return \"Formatting this integer: 1\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringStringFormatWithBooleanLiteral() {
    refactoringHelper
        .addInputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return String.format(\"Formatting this boolean: %B\", true);",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return \"Formatting this boolean: TRUE\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringStringFormatWithStringLiteral() {
    refactoringHelper
        .addInputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return String.format(\"Formatting this string: %s\", \"data\");",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return \"Formatting this string: data\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringStringFormatWithMultipleLiterals() {
    refactoringHelper
        .addInputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return String.format(\"Formatting this string: %s;Integer: %d\", \"data\", 1);",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return \"Formatting this string: data;Integer: 1\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringStringFormatWithLineBreak() {
    refactoringHelper
        .addInputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return String.format(\"hello \\n %s\", \"world\");",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return \"hello \\n world\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringStringFormatWithLineBreakOnLiteral() {
    refactoringHelper
        .addInputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return String.format(\"hello %s\", \"\\n world\");",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return \"hello \\n world\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringStringFormatWithSingleQuoteLiteral() {
    refactoringHelper
        .addInputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return String.format(\"hello %s\", \"['world']\");",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClass.java",
            "public class ExampleClass {",
            "  String test() {",
            "    return \"hello ['world']\";",
            "  }",
            "}")
        .doTest();
  }
}
