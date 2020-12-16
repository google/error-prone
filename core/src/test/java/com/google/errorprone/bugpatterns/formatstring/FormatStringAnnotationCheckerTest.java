/*
 * Copyright 2016 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link FormatStringAnnotationChecker}Test. */
@RunWith(JUnit4.class)
public class FormatStringAnnotationCheckerTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FormatStringAnnotationChecker.class, getClass());

  @Test
  public void testMatches_failsWithNonMatchingFormatArgs() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  @FormatMethod public static void log(@FormatString String s, Object... args) {}",
            "  @FormatMethod public static void callLog(@FormatString String s, Object arg,",
            "      Object arg2) {",
            "    // BUG: Diagnostic contains: The number of format arguments passed with an",
            "    log(s, \"test\");",
            "    // BUG: Diagnostic contains: The format argument types passed with an",
            "    log(s, \"test1\", \"test2\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_succeedsWithMatchingFormatStringAndArgs() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  @FormatMethod public static void log(@FormatString String s, Object... args) {}",
            "  @FormatMethod public static void callLog(@FormatString String s, Object arg) {",
            "    log(s, arg);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_succeedsForMatchingFormatMethodWithImplicitFormatString() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  @FormatMethod public static void log(@FormatString String s, Object... args) {}",
            "  @FormatMethod public static void callLog(String s, Object arg) {",
            "    log(s, arg);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_failsWithMismatchedFormatString() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  @FormatMethod public static void log(@FormatString String s, Object... args) {}",
            "  public static void callLog() {",
            "    // BUG: Diagnostic contains: extra format arguments: used 1, provided 2",
            "    log(\"%s\", new Object(), new Object());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_succeedsForCompileTimeConstantFormatString() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  @FormatMethod public static void log(@FormatString String s, Object... args) {}",
            "  public static void callLog() {",
            "    final String formatString = \"%d\";",
            "    log(formatString, Integer.valueOf(0));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_failsWhenExpressionGivenForFormatString() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "public class FormatStringTestCase {",
            "  @FormatMethod static void log(String s, Object... args) {}",
            "  public static String formatString() { return \"\";}",
            "  public static void callLog() {",
            "    String format = \"log: \";",
            "    // BUG: Diagnostic contains: Format strings must be either literals or",
            "    log(format + 3);",
            "    // BUG: Diagnostic contains: Format strings must be either literals or",
            "    log(formatString());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_failsForInvalidMethodHeaders() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  // BUG: Diagnostic contains: A method cannot have more than one @FormatString",
            "  @FormatMethod void log1(@FormatString String s1, @FormatString String s2) {}",
            "  // BUG: Diagnostic contains: An @FormatMethod must contain at least one String",
            "  @FormatMethod void log2(Object o) {}",
            "  // BUG: Diagnostic contains: Only strings can be annotated @FormatString.",
            "  @FormatMethod void log3(@FormatString Object o) {}",
            "  // BUG: Diagnostic contains: A parameter can only be annotated @FormatString in a",
            "  void log4(@FormatString Object o) {}",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_failsForIncorrectStringParameterUsedWithImplicitFormatString() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  @FormatMethod public static void log(@FormatString String s, Object... args) {}",
            "  @FormatMethod public static void callLog1(String format, String s, Object arg) {",
            "    // BUG: Diagnostic contains: Format strings must be compile time constants or",
            "    log(s, arg);",
            "  }",
            "  @FormatMethod public static void callLog2(String s, @FormatString String format,",
            "      Object arg) {",
            "    // BUG: Diagnostic contains: Format strings must be compile time constants or",
            "    log(s, arg);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_succeedsForNonParameterFinalOrEffectivelyFinalFormatStrings() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  private static final String validFormat = \"foo\";",
            "  @FormatMethod public static void log(@FormatString String s, Object... args) {}",
            "  public static void callLog1() {",
            "    final String fmt1 = \"foo%s\";",
            "    log(fmt1, new Object());",
            // Effectively final
            "    String fmt2 = \"foo%s\";",
            "    log(fmt2, new Object());",
            "    log(validFormat);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_failsForNonFinalParametersOrNonMatchingFinalParameters() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  final String invalidFormat;",
            "  private static final String validFormat = \"foo%s\";",
            "  public FormatStringTestCase() {",
            "    invalidFormat = \"foo\";",
            "  }",
            "  @FormatMethod public static void log(@FormatString String s, Object... args) {}",
            "  public void callLog() {",
            "    final String fmt1 = \"foo%s\";",
            "    // BUG: Diagnostic contains: missing argument for format specifier '%s'",
            "    log(fmt1);",
            // Effectively final
            "    String fmt2 = \"foo%s\";",
            "    // BUG: Diagnostic contains: missing argument for format specifier '%s'",
            "    log(fmt2);",
            // Still effectively final, but multiple assignments is invalid
            "    String fmt3;",
            "    if (true) {",
            "      fmt3 = \"foo%s\";",
            "    } else {",
            "      fmt3 = \"bar%s\";",
            "    }",
            "    // BUG: Diagnostic contains: Variables used as format strings must be initialized",
            "    log(fmt3);",
            "    String fmt4 = fmt3;",
            "    // BUG: Diagnostic contains: Local format string variables must only be assigned",
            "    log(fmt4);",
            "    String fmt5 = \"foo\";",
            // This makes fmt5 no longer effectively final
            "    fmt5 += 'a';",
            "    // BUG: Diagnostic contains: All variables passed as @FormatString must be final",
            "    log(fmt5);",
            "    // BUG: Diagnostic contains: Variables used as format strings that are not local",
            "    log(invalidFormat);",
            "    // BUG: Diagnostic contains: missing argument for format specifier '%s'",
            "    log(validFormat);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_failsForBadCallToConstructor() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  @FormatMethod public FormatStringTestCase(String s, Object... args) {}",
            "  public static void createTestCase(String s, Object arg) {",
            "    // BUG: Diagnostic contains: Format strings must be compile time constants or",
            "    new FormatStringTestCase(s, arg);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_succeedsForMockitoMatchers() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import static org.mockito.ArgumentMatchers.any;",
            "import static org.mockito.ArgumentMatchers.eq;",
            "import static org.mockito.Mockito.verify;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  @FormatMethod public void log(@FormatString String s, Object... args) {}",
            "  public void callLog(String s, Object... args) {",
            "    verify(this).log(any(String.class), eq(args));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_succeedsForMockitoArgumentMatchers() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import static org.mockito.ArgumentMatchers.any;",
            "import static org.mockito.ArgumentMatchers.eq;",
            "import static org.mockito.Mockito.verify;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  @FormatMethod public void log(@FormatString String s, Object... args) {}",
            "  public void callLog(String s, Object... args) {",
            "    verify(this).log(any(String.class), eq(args));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_noFormatString() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "public class FormatStringTestCase {",
            "  // BUG: Diagnostic contains: must contain at least one String parameter",
            "  @FormatMethod public static void log(int x, int y) {}",
            "  void test() { ",
            "    log(1, 2);",
            "  }",
            "}")
        .doTest();
  }
}
