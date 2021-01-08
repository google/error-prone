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

import static org.junit.Assume.assumeTrue;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link StringSplitter} check. */
@RunWith(JUnit4.class)
public class StringSplitterTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new StringSplitter(), getClass());

  @Test
  public void positive() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    for (String s : \"\".split(\":\")) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "class Test {",
            "  void f() {",
            "    for (String s : Splitter.on(':').split(\"\")) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  // Regression test for issue #1124
  @Test
  public void positive_localVarTypeInference() {
    assumeTrue(RuntimeVersion.isAtLeast10());
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    var lines = \"\".split(\":\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "class Test {",
            "  void f() {",
            "    var lines = Splitter.on(':').split(\"\");",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void positive_patternIsSymbol() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  static final String NON_REGEX_PATTERN_STRING = \":\";",
            "  static final String REGEX_PATTERN_STRING = \".*\";",
            "  static final String CONVERTIBLE_PATTERN_STRING = \"\\\\Q\\\\E:\";",
            "  void f() {",
            "    for (String s : \"\".split(NON_REGEX_PATTERN_STRING)) {}",
            "    for (String s : \"\".split(REGEX_PATTERN_STRING)) {}",
            "    for (String s : \"\".split(CONVERTIBLE_PATTERN_STRING)) {}",
            "    for (String s : \"\".split((CONVERTIBLE_PATTERN_STRING))) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "class Test {",
            "  static final String NON_REGEX_PATTERN_STRING = \":\";",
            "  static final String REGEX_PATTERN_STRING = \".*\";",
            "  static final String CONVERTIBLE_PATTERN_STRING = \"\\\\Q\\\\E:\";",
            "  void f() {",
            "    for (String s : Splitter.onPattern(NON_REGEX_PATTERN_STRING).split(\"\")) {}",
            "    for (String s : Splitter.onPattern(REGEX_PATTERN_STRING).split(\"\")) {}",
            "    for (String s : Splitter.onPattern(CONVERTIBLE_PATTERN_STRING).split(\"\")) {}",
            "    for (String s : Splitter.onPattern((CONVERTIBLE_PATTERN_STRING)).split(\"\")) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void positive_patternIsConcatenation() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    for (String s : \"\".split(\":\" + 0)) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "class Test {",
            "  void f() {",
            "    for (String s : Splitter.onPattern(\":\" + 0).split(\"\")) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void positive_patternNotConstant() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String pattern = \":\";",
            "    for (String s : \"\".split(pattern)) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "class Test {",
            "  void f() {",
            "    String pattern = \":\";",
            "    for (String s : Splitter.onPattern(pattern).split(\"\")) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void positive_singleEscapedCharacter() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    for (String s : \"\".split(\"\\u0000\")) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "class Test {",
            "  void f() {",
            "    for (String s : Splitter.on('\\u0000').split(\"\")) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void varLoop() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String[] pieces = \"\".split(\":\");",
            "    for (String s : pieces) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "class Test {",
            "  void f() {",
            "    Iterable<String> pieces = Splitter.on(':').split(\"\");",
            "    for (String s : pieces) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void varLoopLength() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String[] pieces = \"\".split(\":\");",
            "    for (int i = 0; i < pieces.length; i++) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "import java.util.List;",
            "class Test {",
            "  void f() {",
            "    List<String> pieces = Splitter.on(':').splitToList(\"\");",
            "    for (int i = 0; i < pieces.size(); i++) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void varList() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String[] pieces = \"\".split(\":\");",
            "    System.err.println(pieces[0]);",
            "    System.err.println(pieces[1]);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "import java.util.List;",
            "class Test {",
            "  void f() {",
            "    List<String> pieces = Splitter.on(':').splitToList(\"\");",
            "    System.err.println(pieces.get(0));",
            "    System.err.println(pieces.get(1));",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void positiveRegex() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    for (String s : \"\".split(\".*foo\\\\t\")) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "class Test {",
            "  void f() {",
            "    for (String s : Splitter.onPattern(\".*foo\\\\t\").split(\"\")) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void character() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    for (String s : \"\".split(\"c\")) {}",
            "    for (String s : \"\".split(\"abc\")) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "class Test {",
            "  void f() {",
            "    for (String s : Splitter.on('c').split(\"\")) {}",
            "    for (String s : Splitter.on(\"abc\").split(\"\")) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void negative() {
    CompilationTestHelper.newInstance(StringSplitter.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String[] pieces = \"\".split(\":\");",
            "    System.err.println(pieces);", // escapes
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mutation() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String[] xs = \"\".split(\"c\");",
            "    xs[0] = null;",
            "    System.err.println(xs[0]);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  void f() {",
            "    List<String> xs = new ArrayList<>(Splitter.on('c').splitToList(\"\"));",
            "    xs.set(0, null);",
            "    System.err.println(xs.get(0));",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  // regression test for b/72088500
  @Test
  public void b72088500() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f(String input) {",
            "    String[] lines = input.split(\"\\\\r?\\\\n\");",
            "    System.err.println(lines[0]);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "import java.util.List;",
            "class Test {",
            "  void f(String input) {",
            "    List<String> lines = Splitter.onPattern(\"\\\\r?\\\\n\").splitToList(input);",
            "    System.err.println(lines.get(0));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void escape() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String[] pieces = \"\".split(\"\\n\\t\\r\\f\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "class Test {",
            "  void f() {",
            "    Iterable<String> pieces = Splitter.on(\"\\n\\t\\r\\f\").split(\"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immediateArrayAccess() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String x = \"\".split(\"c\")[0];",
            "    x = \"\".split(\"c\")[1];",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "import com.google.common.collect.Iterables;",
            "class Test {",
            "  void f() {",
            "    String x = Iterables.get(Splitter.on('c').split(\"\"), 0);",
            "    x = Iterables.get(Splitter.on('c').split(\"\"), 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testStringSplitPositive() {
    CompilationTestHelper.newInstance(StringSplitter.class, getClass())
        .addSourceFile("StringSplitterPositiveCases.java")
        .doTest();
  }

  @Test
  public void testStringSplitNegative() {
    CompilationTestHelper.newInstance(StringSplitter.class, getClass())
        .addSourceFile("StringSplitterNegativeCases.java")
        .doTest();
  }

  @Ignore("b/112270644")
  @Test
  public void noSplitterOnClassPath() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    for (String s : \"\".split(\":\")) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    for (String s : \"\".split(\":\", -1)) {}",
            "  }",
            "}")
        .setArgs("-cp", ":")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void patternSplit() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  void f() {",
            "    String x = Pattern.compile(\"\").split(\"c\")[0];",
            "    for (String s : Pattern.compile(\"\").split(\":\")) {}",
            "    String[] xs = Pattern.compile(\"c\").split(\"\");",
            "    xs[0] = null;",
            "    System.err.println(xs[0]);",
            "    String[] pieces = Pattern.compile(\":\").split(\"\");",
            "    for (int i = 0; i < pieces.length; i++) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Splitter;",
            "import com.google.common.collect.Iterables;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "import java.util.regex.Pattern;",
            "",
            "class Test {",
            "  void f() {",
            "    String x = Iterables.get(Splitter.on(Pattern.compile(\"\")).split(\"c\"), 0);",
            "    for (String s : Splitter.on(Pattern.compile(\"\")).split(\":\")) {}",
            "    List<String> xs ="
                + " new ArrayList<>(Splitter.on(Pattern.compile(\"c\")).splitToList(\"\"));",
            "    xs.set(0, null);",
            "    System.err.println(xs.get(0));",
            "    List<String> pieces = Splitter.on(Pattern.compile(\":\")).splitToList(\"\");",
            "    for (int i = 0; i < pieces.size(); i++) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }
}
