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

import static org.junit.Assume.assumeTrue;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link FormatString}Test */
@RunWith(JUnit4.class)
public class FormatStringTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FormatString.class, getClass());

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
    testFormat("use %d to format integral types", "String.format(\"%l\", 42);");
  }

  @Test
  public void testCStyleLongConversion2() throws Exception {
    testFormat("use %d to format integral types", "String.format(\"%ld\", 42);");
  }

  @Test
  public void testCStyleLongConversion3() throws Exception {
    testFormat("use %d to format integral types", "String.format(\"%lld\", 42);");
  }

  @Test
  public void testCStyleLongConversion4() throws Exception {
    testFormat("%f, %g or %e to format floating point types", "String.format(\"%lf\", 42);");
  }

  @Test
  public void testCStyleLongConversion5() throws Exception {
    testFormat("%f, %g or %e to format floating point types", "String.format(\"%llf\", 42);");
  }

  @Test
  public void testConditionalExpression() throws Exception {
    testFormat(
        "missing argument for format specifier '%s'", "String.format(true ? \"\" : \"%s\");");
  }

  @Test
  public void testConditionalExpression2() throws Exception {
    testFormat(
        "missing argument for format specifier '%s'", "String.format(true ? \"%s\" : \"\");");
  }

  @Test
  public void testConditionalExpression3() throws Exception {
    testFormat(
        "extra format arguments: used 1, provided 2",
        "String.format(true ? \"%s\" : \"%s\", 1, 2);");
  }

  @Test
  public void testConditionalExpression4() throws Exception {
    testFormat(
        "extra format arguments: used 1, provided 2",
        "String.format(true ? \"%s\" : \"%s\", 1, 2);");
  }

  @Test
  public void testConditionalExpression5() throws Exception {
    testFormat(
        "missing argument for format specifier '%s'",
        "String.format(true ? \"%s\" : true ? \"%s\" : \"\");");
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
  public void testPrintfMethods_StringFormat() throws Exception {
    testFormat("", "String.format(\"%d\", \"hello\");");
  }

  @Test
  public void testPrintfMethods_StringFormatWithLocale() throws Exception {
    testFormat("", "String.format(Locale.ENGLISH, \"%d\", \"hello\");");
  }

  @Test
  public void testPrintfMethods_PrintWriterFormat() throws Exception {
    testFormat("", "new PrintWriter(System.err).format(\"%d\", \"hello\");");
  }

  @Test
  public void testPrintfMethods_PrintWriterFormatWithLocale() throws Exception {
    testFormat("", "new PrintWriter(System.err).format(Locale.ENGLISH, \"%d\", \"hello\");");
  }

  @Test
  public void testPrintfMethods_PrintWriterPrintf() throws Exception {
    testFormat("", "new PrintWriter(System.err).printf(\"%d\", \"hello\");");
  }

  @Test
  public void testPrintfMethods_PrintWriterPrintfWithLocale() throws Exception {
    testFormat("", "new PrintWriter(System.err).printf(Locale.ENGLISH, \"%d\", \"hello\");");
  }

  @Test
  public void testPrintfMethods_PrintStreamFormat() throws Exception {
    testFormat("", "new PrintStream(System.err).format(\"%d\", \"hello\");");
  }

  @Test
  public void testPrintfMethods_PrintStreamFormatWithLocale() throws Exception {
    testFormat("", "new PrintStream(System.err).format(Locale.ENGLISH, \"%d\", \"hello\");");
  }

  @Test
  public void testPrintfMethods_PrintStreamPrintf() throws Exception {
    testFormat("", "new PrintStream(System.err).printf(\"%d\", \"hello\");");
  }

  @Test
  public void testPrintfMethods_PrintStreamPrintfWithLocale() throws Exception {
    testFormat("", "new PrintStream(System.err).printf(Locale.ENGLISH, \"%d\", \"hello\");");
  }

  @Test
  public void testPrintfMethods_FormatterFormatWithLocale() throws Exception {
    testFormat(
        "", "new java.util.Formatter(System.err).format(Locale.ENGLISH, \"%d\", \"hello\");");
  }

  @Test
  public void testPrintfMethods_ConsolePrintf() throws Exception {
    testFormat("", "System.console().printf(\"%d\", \"hello\");");
  }

  @Test
  public void testPrintfMethods_ConsoleFormat() throws Exception {
    testFormat("", "System.console().format(\"%d\", \"hello\");");
  }

  @Test
  public void testPrintfMethods_ConsoleFormat_noErrorsWithEmptyArgs() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    System.console().readLine();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPrintfMethods_ConsoleReadline() throws Exception {
    testFormat("", "System.console().readLine(\"%d\", \"hello\");");
  }

  @Test
  public void testPrintfMethods_ConsoleReadPassword() throws Exception {
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
            "import java.time.*;",
            "class Test {",
            "  void f() {",
            "    System.err.printf(\"%tY\",",
            "        LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));",
            "    System.err.printf(\"%tQ\", Instant.now());",
            "    System.err.printf(\"%tZ\", ZonedDateTime.of(LocalDate.of(2018, 12, 27),"
                + " LocalTime.of(17, 0), ZoneId.of(\"Europe/London\")));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void number() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(Number n) {",
            "    System.err.printf(\"%x\", n);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void invalidIndex() {
    assumeTrue(RuntimeVersion.isAtLeast16());
    compilationHelper
        .addSourceLines(
            "T.java",
            "class T {",
            "  public static void main(String[] args) {",
            "    // BUG: Diagnostic contains: Illegal format argument index",
            "    System.err.printf(\" %0$2s) %s\", args[0], args[1]);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stringFormattedNegativeCase() {
    assumeTrue(RuntimeVersion.isAtLeast15());
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    \"%s %s\".formatted(\"foo\", \"baz\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stringFormattedPositiveCase() {
    assumeTrue(RuntimeVersion.isAtLeast15());
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: missing argument for format specifier",
            "    \"%s %s %s\".formatted(\"foo\", \"baz\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonConstantStringFormattedNegativeCase() {
    assumeTrue(RuntimeVersion.isAtLeast15());
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(String f) {",
            "    f.formatted(\"foo\", \"baz\");",
            "  }",
            "}")
        .doTest();
  }
}
