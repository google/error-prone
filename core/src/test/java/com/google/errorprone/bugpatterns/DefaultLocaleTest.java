/*
 * Copyright 2024 The Error Prone Authors.
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

import static com.google.common.base.Predicates.containsPattern;
import static com.google.common.truth.TruthJUnit.assume;
import static com.google.errorprone.bugpatterns.DefaultLocale.onlyContainsSpecifiersInAllowList;
import static java.util.function.Predicate.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link DefaultLocale}Test */
@RunWith(JUnit4.class)
public class DefaultLocaleTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(DefaultLocale.class, getClass());

  private BugCheckerRefactoringTestHelper refactoringTest() {
    return BugCheckerRefactoringTestHelper.newInstance(DefaultLocale.class, getClass());
  }

  @Test
  public void testOnlyContainsSpecifiersInAllowList() {
    assertTrue(onlyContainsSpecifiersInAllowList("%%%n%b%h%c%s"));
    assertTrue(onlyContainsSpecifiersInAllowList("%1$s%<s%1s%1.2s%2$-3.4s%<-42s"));
    // Implies a Formattable argument, so no need to match here
    assertFalse(onlyContainsSpecifiersInAllowList("%#s"));
    // Use locale-aware uppercase
    assertFalse(onlyContainsSpecifiersInAllowList("%S"));
    assertFalse(onlyContainsSpecifiersInAllowList("%B"));
    assertFalse(onlyContainsSpecifiersInAllowList("%H"));
    assertFalse(onlyContainsSpecifiersInAllowList("%C"));
    // Use locale-aware formatting
    assertFalse(onlyContainsSpecifiersInAllowList("%d"));
    assertFalse(onlyContainsSpecifiersInAllowList("%o"));
    assertFalse(onlyContainsSpecifiersInAllowList("%x"));
    assertFalse(onlyContainsSpecifiersInAllowList("%X"));
    assertFalse(onlyContainsSpecifiersInAllowList("%e"));
    assertFalse(onlyContainsSpecifiersInAllowList("%E"));
    assertFalse(onlyContainsSpecifiersInAllowList("%f"));
    assertFalse(onlyContainsSpecifiersInAllowList("%g"));
    assertFalse(onlyContainsSpecifiersInAllowList("%G"));
    assertFalse(onlyContainsSpecifiersInAllowList("%a"));
    assertFalse(onlyContainsSpecifiersInAllowList("%A"));
    assertFalse(onlyContainsSpecifiersInAllowList("%tc"));
    assertFalse(onlyContainsSpecifiersInAllowList("%Tc"));
  }

  @Test
  public void formatMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.*;",
            "import java.text.*;",
            "import java.util.Formattable;",
            "class Test {",
            "  static final String PATTERN = \"%d\";",
            "  static abstract class F implements Formattable {};",
            "  void f(PrintStream ps, PrintWriter pw, String pattern, F formattable) throws"
                + " Exception {",
            "    // BUG: Diagnostic contains: ps.format(Locale.getDefault(FORMAT), PATTERN, 42);",
            "    ps.format(PATTERN, 42);",
            "    // BUG: Diagnostic contains: ps.format(Locale.getDefault(FORMAT), \"%s\","
                + " formattable);",
            "    ps.format(\"%s\", formattable);",
            "    // BUG: Diagnostic contains: ps.format(Locale.getDefault(FORMAT), pattern,"
                + " formattable);",
            "    ps.format(pattern, formattable);",
            "    // BUG: Diagnostic contains: ps.format(Locale.getDefault(FORMAT), \"%d\", 42);",
            "    ps.format(\"%d\", 42);",
            "    // BUG: Diagnostic contains: ps.printf(Locale.getDefault(FORMAT), \"%d\", 42);",
            "    ps.printf(\"%d\", 42);",
            "    // BUG: Diagnostic contains: pw.format(Locale.getDefault(FORMAT), \"%d\", 42);",
            "    pw.format(\"%d\", 42);",
            "    // BUG: Diagnostic contains: pw.printf(Locale.getDefault(FORMAT), \"%d\", 42);",
            "    pw.printf(\"%d\", 42);",
            "    // BUG: Diagnostic contains: String.format(Locale.getDefault(FORMAT), \"%d\","
                + " 42);",
            "    String.format(\"%d\", 42);",
            "    // BUG: Diagnostic contains: new MessageFormat(\"%d\","
                + " Locale.getDefault(FORMAT)).format(42);",
            "    MessageFormat.format(\"%d\", 42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void formatMethods_negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.*;",
            "import java.io.*;",
            "import java.util.Formattable;",
            "class Test {",
            "  static final String PATTERN = \"%s\";",
            "  final static class B {}",
            "  void f(String s, int i, B b) throws Exception {",
            // On System.out and System.err
            "    System.out.format(\"%d\", 42);",
            "    System.out.printf(\"%d\", 42);",
            "    System.err.format(\"%d\", 42);",
            "    System.err.printf(\"%d\", 42);",
            // "Safe" String.format
            "    String.format(PATTERN, 42);",
            "    String.format(\"%s\", \"literal\");",
            "    String.format(\"%c\", 42);",
            "    String.format(\"%s\", s);",
            "    String.format(\"%c\", i);",
            "    String.format(\"%1$s %<h %<b\", b);",
            "    MessageFormat.format(\"%s\", s);",
            // Let non-constants pass
            "    String.format(s, 42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stringFormatted() {
    assume().that(Runtime.version().feature()).isAtLeast(15);
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Formattable;",
            "class Test {",
            "  private abstract class F implements Formattable {}",
            "  final class B {}",
            "  void f(String s, int i, F f, B b) throws Exception {",
            "    // BUG: Diagnostic contains: String.format(Locale.getDefault(FORMAT), \"%d\","
                + " 42);",
            "    \"%d\".formatted(42);",
            "    // BUG: Diagnostic contains: String.format(Locale.getDefault(FORMAT), \"%s\", f);",
            "    \"%s\".formatted(f);",
            // Negative:
            "    \"%s\".formatted(\"literal\");",
            "    \"%c\".formatted(42);",
            "    \"%s\".formatted(s);",
            "    \"%c\".formatted(i);",
            "    \"%1$s %<h %<b\".formatted(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void displayMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.*;",
            "class Test {",
            "  void f(Currency currency) throws Exception {",
            "    // BUG: Diagnostic contains: currency.getSymbol(Locale.getDefault(DISPLAY));",
            "    currency.getSymbol();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void factoryMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.*;",
            "import java.time.format.*;",
            "class Test {",
            "  void f(DateTimeFormatterBuilder dtfb) throws Exception {",
            "    // BUG: Diagnostic contains:"
                + " BreakIterator.getCharacterInstance(Locale.getDefault());",
            "    BreakIterator.getCharacterInstance();",
            "    // BUG: Diagnostic contains: BreakIterator.getLineInstance(Locale.getDefault());",
            "    BreakIterator.getLineInstance();",
            "    // BUG: Diagnostic contains:"
                + " BreakIterator.getSentenceInstance(Locale.getDefault());",
            "    BreakIterator.getSentenceInstance();",
            "    // BUG: Diagnostic contains: BreakIterator.getWordInstance(Locale.getDefault());",
            "    BreakIterator.getWordInstance();",
            "    // BUG: Diagnostic contains: Collator.getInstance(Locale.getDefault());",
            "    Collator.getInstance();",
            "    // BUG: Diagnostic contains:"
                + " NumberFormat.getCurrencyInstance(Locale.getDefault(FORMAT));",
            "    NumberFormat.getCurrencyInstance();",
            "    // BUG: Diagnostic contains: NumberFormat.getInstance(Locale.getDefault(FORMAT));",
            "    NumberFormat.getInstance();",
            "    // BUG: Diagnostic contains:"
                + " NumberFormat.getIntegerInstance(Locale.getDefault(FORMAT));",
            "    NumberFormat.getIntegerInstance();",
            "    // BUG: Diagnostic contains:"
                + " NumberFormat.getNumberInstance(Locale.getDefault(FORMAT));",
            "    NumberFormat.getNumberInstance();",
            "    // BUG: Diagnostic contains:"
                + " NumberFormat.getPercentInstance(Locale.getDefault(FORMAT));",
            "    NumberFormat.getPercentInstance();",
            "    // BUG: Diagnostic contains:"
                + " DateFormatSymbols.getInstance(Locale.getDefault(FORMAT));",
            "    DateFormatSymbols.getInstance();",
            "    // BUG: Diagnostic contains:"
                + " DecimalFormatSymbols.getInstance(Locale.getDefault(FORMAT));",
            "    DecimalFormatSymbols.getInstance();",
            "    // BUG: Diagnostic contains: DateTimeFormatter.ofPattern(\"pattern\","
                + " Locale.getDefault(FORMAT));",
            "    DateTimeFormatter.ofPattern(\"pattern\");",
            "    // BUG: Diagnostic contains: dtfb.toFormatter(Locale.getDefault(FORMAT));",
            "    dtfb.toFormatter();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void factoryMethodsJdk12plus() {
    assume().that(Runtime.version().feature()).isAtLeast(12);
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.*;",
            "class Test {",
            "  void f() throws Exception {",
            "    // BUG: Diagnostic contains:"
                + " NumberFormat.getCompactNumberInstance(Locale.getDefault(FORMAT));",
            "    NumberFormat.getCompactNumberInstance();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void dateFormat() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static java.text.DateFormat.*;",
            "import java.text.*;",
            "class Test {",
            "  void f() throws Exception {",
            "    // BUG: Diagnostic contains: DateFormat.getDateTimeInstance(SHORT, SHORT,"
                + " Locale.getDefault(FORMAT));",
            "    DateFormat.getInstance();",
            "    // BUG: Diagnostic contains: DateFormat.getDateInstance(DEFAULT,"
                + " Locale.getDefault(FORMAT));",
            "    DateFormat.getDateInstance();",
            "    // BUG: Diagnostic contains: DateFormat.getDateInstance(SHORT,"
                + " Locale.getDefault(FORMAT));",
            "    DateFormat.getDateInstance(SHORT);",
            "    // BUG: Diagnostic contains: DateFormat.getTimeInstance(DEFAULT,"
                + " Locale.getDefault(FORMAT));",
            "    DateFormat.getTimeInstance();",
            "    // BUG: Diagnostic contains: DateFormat.getTimeInstance(SHORT,"
                + " Locale.getDefault(FORMAT));",
            "    DateFormat.getTimeInstance(SHORT);",
            "    // BUG: Diagnostic contains: DateFormat.getDateTimeInstance(DEFAULT, DEFAULT,"
                + " Locale.getDefault(FORMAT));",
            "    DateFormat.getDateTimeInstance();",
            "    // BUG: Diagnostic contains: DateFormat.getDateTimeInstance(SHORT, LONG,"
                + " Locale.getDefault(FORMAT));",
            "    DateFormat.getDateTimeInstance(SHORT, LONG);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void resourceBundle() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.*;",
            "class Test {",
            "  void f(ResourceBundle.Control control, Locale locale, ClassLoader cl) throws"
                + " Exception {",
            "    // BUG: Diagnostic contains: ResourceBundle.getBundle(\"name\","
                + " Locale.getDefault());",
            "    ResourceBundle.getBundle(\"name\");",
            "    // BUG: Diagnostic contains: ResourceBundle.getBundle(\"name\","
                + " Locale.getDefault(), control);",
            "    ResourceBundle.getBundle(\"name\", control);",
            // negative
            "    ResourceBundle.getBundle(\"name\", locale);",
            "    ResourceBundle.getBundle(\"name\", locale, control);",
            "    ResourceBundle.getBundle(\"name\", locale, cl);",
            "    ResourceBundle.getBundle(\"name\", locale, cl, control);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void resourceBundleJdk9plus() {
    assume().that(Runtime.version().feature()).isAtLeast(9);
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.*;",
            "class Test {",
            "  void f(ResourceBundle.Control control, Locale locale, Module module) throws"
                + " Exception {",
            "    // BUG: Diagnostic contains: ResourceBundle.getBundle(\"name\","
                + " Locale.getDefault(), module);",
            "    ResourceBundle.getBundle(\"name\", module);",
            // negative
            "    ResourceBundle.getBundle(\"name\", locale, module);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void formatConstructors() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.*;",
            "import java.text.*;",
            "class Test {",
            "  void f() throws Exception {",
            "    // BUG: Diagnostic contains: new MessageFormat(\"%d\","
                + " Locale.getDefault(FORMAT));",
            "    new MessageFormat(\"%d\");",
            "    // BUG: Diagnostic contains: new DateFormatSymbols(Locale.getDefault(FORMAT));",
            "    new DateFormatSymbols();",
            "    // BUG: Diagnostic contains: new DecimalFormatSymbols(Locale.getDefault(FORMAT));",
            "    new DecimalFormatSymbols();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void decimalFormat() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.*;",
            "class Test {",
            "  void f(DecimalFormatSymbols dfs) throws Exception {",
            "    // BUG: Diagnostic contains: NumberFormat.getInstance(Locale.getDefault(FORMAT));",
            "    new DecimalFormat();",
            "    // BUG: Diagnostic contains: new DecimalFormat(\"000\","
                + " DecimalFormatSymbols.getInstance(Locale.getDefault(FORMAT)));",
            "    new DecimalFormat(\"000\");",
            // negative
            "    new DecimalFormat(\"000\", dfs);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void simpleDateFormat() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.*;",
            "import java.util.*;",
            "class Test {",
            "  void f(Locale locale, DateFormatSymbols dfs) throws Exception {",
            "    // BUG: Diagnostic contains: DateFormat.getDateTimeInstance(SHORT, SHORT,"
                + " Locale.getDefault(FORMAT));",
            "    new SimpleDateFormat();",
            "    // BUG: Diagnostic contains: new SimpleDateFormat(\"yMd\","
                + " Locale.getDefault(FORMAT));",
            "    new SimpleDateFormat(\"yMd\");",
            // negative
            "    new SimpleDateFormat(\"yMd\", locale);",
            "    new SimpleDateFormat(\"yMd\", dfs);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void formatter() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static java.nio.charset.StandardCharsets.*;",
            "import java.io.*;",
            "import java.text.*;",
            "import java.util.*;",
            "class Test {",
            "  void f(Locale locale, File file, OutputStream os) throws Exception {",
            "    // BUG: Diagnostic contains: new Formatter(Locale.getDefault(FORMAT));",
            "    new Formatter();",
            "    // BUG: Diagnostic contains: new Formatter(new StringBuilder(),"
                + " Locale.getDefault(FORMAT));",
            "    new Formatter(new StringBuilder());",
            "    // BUG: Diagnostic matches: NoFix",
            "    new Formatter(\"filename\");",
            "    // BUG: Diagnostic contains: new Formatter(\"filename\", \"utf8\","
                + " Locale.getDefault(FORMAT));",
            "    new Formatter(\"filename\", \"utf8\");",
            "    // BUG: Diagnostic matches: NoFix",
            "    new Formatter(file);",
            "    // BUG: Diagnostic contains: new Formatter(file, \"utf8\","
                + " Locale.getDefault(FORMAT));",
            "    new Formatter(file, \"utf8\");",
            "    // BUG: Diagnostic matches: NoFix",
            "    new Formatter(System.out);",
            "    // BUG: Diagnostic matches: NoFix",
            "    new Formatter(os);",
            "    // BUG: Diagnostic contains: new Formatter(os, \"utf8\","
                + " Locale.getDefault(FORMAT));",
            "    new Formatter(os, \"utf8\");",
            // negative
            "    new Formatter(locale);",
            "    new Formatter(new StringBuilder(), locale);",
            "    new Formatter(\"filename\", \"utf8\", locale);",
            "    new Formatter(\"filename\", UTF_8, locale);",
            "    new Formatter(file, \"utf8\", locale);",
            "    new Formatter(file, UTF_8, locale);",
            "    new Formatter(os, \"utf8\", locale);",
            "    new Formatter(os, UTF_8, locale);",
            "  }",
            "}")
        .expectErrorMessage("NoFix", not(containsPattern("Did you mean")))
        .doTest();
  }

  @Test
  public void refactoringAddLocaleImport() {
    refactoringTest()
        .addInputLines(
            "Test.java",
            "import java.text.*;",
            "class Test {",
            "  void f() throws Exception {",
            "    MessageFormat.format(\"%d\", 42);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.text.*;",
            "import java.util.Locale;",
            "class Test {",
            "  void f() throws Exception {",
            "    new MessageFormat(\"%d\", Locale.ROOT).format(42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringAddLocaleCategoryFormatStaticImport() {
    refactoringTest()
        .addInputLines(
            "Test.java",
            "import java.text.*;",
            "class Test {",
            "  void f() throws Exception {",
            "    MessageFormat.format(\"%d\", 42);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static java.util.Locale.Category.FORMAT;",
            "import java.text.*;",
            "import java.util.Locale;",
            "class Test {",
            "  void f() throws Exception {",
            "    new MessageFormat(\"%d\", Locale.getDefault(FORMAT)).format(42);",
            "  }",
            "}")
        .setFixChooser(FixChoosers.THIRD)
        .doTest();
  }
}
