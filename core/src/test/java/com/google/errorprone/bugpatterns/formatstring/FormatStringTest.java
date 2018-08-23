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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link FormatString}Test */
@RunWith(JUnit4.class)
public class FormatStringTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(FormatString.class, getClass());
  }

  private void testFormat(String expected, String formatString) {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Locale;",
            "import java.io.PrintWriter;",
            "import java.io.PrintStream;",
            "import java.io.Console;",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: " + expected,
            "    " + formatString,
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDuplicateFormatFlags() throws Exception {
    testFormat("duplicate format flags: +", "String.format(\"e = %++10.4f\", Math.E);");
  }

  @Test
  public void testFormatFlagsConversionMismatch() throws Exception {
    testFormat(
        "format specifier '%b' is not compatible with the given flag(s): #",
        "String.format(\"%#b\", Math.E);");
  }

  @Test
  public void testIllegalFormatCodePoint() throws Exception {
    testFormat("invalid Unicode code point: 110000", "String.format(\"%c\", 0x110000);");
  }

  @Test
  public void testIllegalFormatConversion() throws Exception {
    testFormat(
        "illegal format conversion: 'java.lang.String' cannot be formatted using '%f'",
        "String.format(\"%f\", \"abcd\");");
  }

  @Test
  public void testIllegalFormatFlags() throws Exception {
    testFormat("illegal format flags: -0", "String.format(\"%-010d\", 5);");
  }

  @Test
  public void testIllegalFormatPrecision() throws Exception {
    testFormat("illegal format precision: 1", "String.format(\"%.1c\", 'c');");
  }

  @Test
  public void testIllegalFormatWidth() throws Exception {
    testFormat("illegal format width: 1", "String.format(\"%1n\");");
  }

  @Test
  public void testMissingFormatArgument() throws Exception {
    testFormat("missing argument for format specifier '%<s'", "String.format(\"%<s\", \"test\");");
  }

  @Test
  public void testMissingFormatWidth() throws Exception {
    testFormat("missing format width: %-f", "String.format(\"e = %-f\", Math.E);");
  }

  @Test
  public void testUnknownFormatConversion() throws Exception {
    testFormat("unknown format conversion: 'r'", "String.format(\"%r\", \"hello\");");
  }

  @Test
  public void testCStyleLongConversion() throws Exception {
    testFormat("use %d for all integral types", "String.format(\"%l\", 42);");
    testFormat("use %d for all integral types", "String.format(\"%ld\", 42);");
    testFormat("use %d for all integral types", "String.format(\"%lld\", 42);");
    testFormat("%f for all floating point ", "String.format(\"%lf\", 42);");
    testFormat("%f for all floating point ", "String.format(\"%llf\", 42);");
  }

  @Test
  public void testConditionalExpression() throws Exception {
    testFormat(
        "missing argument for format specifier '%s'", "String.format(true ? \"\" : \"%s\");");
    testFormat(
        "missing argument for format specifier '%s'", "String.format(true ? \"%s\" : \"\");");
    testFormat(
        "extra format arguments: used 1, provided 2",
        "String.format(true ? \"%s\" : \"%s\", 1, 2);");
    testFormat(
        "extra format arguments: used 1, provided 2",
        "String.format(true ? \"%s\" : \"%s\", 1, 2);");
  }

  @Test
  public void missingArguments() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: missing argument for format specifier '%s'",
            "    String.format(\"%s %s %s\", 42);",
            "    // BUG: Diagnostic contains: missing argument for format specifier '%s'",
            "    String.format(\"%s %s %s\", 42, 42);",
            "    String.format(\"%s %s %s\", 42, 42, 42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void extraArguments() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String.format(\"%s %s\", 1, 2);",
            "    // BUG: Diagnostic contains: extra format arguments: used 2, provided 3",
            "    String.format(\"%s %s\", 1, 2, 3);",
            "    // BUG: Diagnostic contains: extra format arguments: used 2, provided 4",
            "    String.format(\"%s %s\", 1, 2, 3, 4);",
            "    // BUG: Diagnostic contains: extra format arguments: used 0, provided 1",
            "    String.format(\"{0}\", 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(boolean b) {",
            "    String.format(\"%d\", 42);",
            "    String.format(\"%d\", 42L);",
            "    String.format(\"%f\", 42.0f);",
            "    String.format(\"%f\", 42.0d);",
            "    String.format(\"%s\", \"hello\");",
            "    String.format(\"%s\", 42);",
            "    String.format(\"%s\", (Object) null);",
            "    String.format(\"%s\", new Object());",
            "    String.format(\"%c\", 'c');",
            "    String.format(b ? \"%s\" : \"%d\", 42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPrintfMethods() throws Exception {
    testFormat("", "String.format(\"%d\", \"hello\");");
    testFormat("", "String.format(Locale.ENGLISH, \"%d\", \"hello\");");
    testFormat("", "new PrintWriter(System.err).format(\"%d\", \"hello\");");
    testFormat("", "new PrintWriter(System.err).format(Locale.ENGLISH, \"%d\", \"hello\");");
    testFormat("", "new PrintWriter(System.err).printf(\"%d\", \"hello\");");
    testFormat("", "new PrintWriter(System.err).printf(Locale.ENGLISH, \"%d\", \"hello\");");
    testFormat("", "new PrintStream(System.err).format(\"%d\", \"hello\");");
    testFormat("", "new PrintStream(System.err).format(Locale.ENGLISH, \"%d\", \"hello\");");
    testFormat("", "new PrintStream(System.err).printf(\"%d\", \"hello\");");
    testFormat("", "new PrintStream(System.err).printf(Locale.ENGLISH, \"%d\", \"hello\");");
    testFormat(
        "", "new java.util.Formatter(System.err).format(Locale.ENGLISH, \"%d\", \"hello\");");
    testFormat("", "System.console().printf(\"%d\", \"hello\");");
    testFormat("", "System.console().format(\"%d\", \"hello\");");
    testFormat("", "System.console().readLine(\"%d\", \"hello\");");
    testFormat("", "System.console().readPassword(\"%d\", \"hello\");");
  }

  @Test
  public void nullArgument() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String.format(\"%s %s\", null, null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void javaUtilTime() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.time.Instant;",
            "import java.time.LocalDateTime;",
            "import java.time.ZoneId;",
            "class Test {",
            "  void f() {",
            "    System.err.printf(\"%tY\",",
            "        LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));",
            "  }",
            "}")
        .doTest();
  }
}
