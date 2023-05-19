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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link StringCaseLocaleUsage}. */
@RunWith(JUnit4.class)
public final class StringCaseLocaleUsageTest {
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(StringCaseLocaleUsage.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(StringCaseLocaleUsage.class, getClass());

  @Test
  public void identification() {
    compilationTestHelper
        .addSourceLines(
            "A.java",
            "import static java.util.Locale.ROOT;",
            "",
            "import java.util.Locale;",
            "",
            "class A {",
            "  void m() {",
            "    \"a\".toLowerCase(Locale.ROOT);",
            "    \"a\".toUpperCase(Locale.ROOT);",
            "    \"b\".toLowerCase(ROOT);",
            "    \"b\".toUpperCase(ROOT);",
            "    \"c\".toLowerCase(Locale.getDefault());",
            "    \"c\".toUpperCase(Locale.getDefault());",
            "    \"d\".toLowerCase(Locale.ENGLISH);",
            "    \"d\".toUpperCase(Locale.ENGLISH);",
            "    \"e\".toLowerCase(new Locale(\"foo\"));",
            "    \"e\".toUpperCase(new Locale(\"foo\"));",
            "",
            "    // BUG: Diagnostic contains:",
            "    \"f\".toLowerCase();",
            "    // BUG: Diagnostic contains:",
            "    \"g\".toUpperCase();",
            "",
            "    String h = \"h\";",
            "    // BUG: Diagnostic contains:",
            "    h.toLowerCase();",
            "    String i = \"i\";",
            "    // BUG: Diagnostic contains:",
            "    i.toUpperCase();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void replacementFirstSuggestedFix() {
    refactoringTestHelper
        .addInputLines(
            "A.java",
            "class A {",
            "  void m() {",
            "    \"a\".toLowerCase(/* Comment with parens: (). */ );",
            "    \"b\".toUpperCase();",
            "    \"c\".toLowerCase().toString();",
            "",
            "    toString().toLowerCase();",
            "    toString().toUpperCase /* Comment with parens: (). */();",
            "",
            "    this.toString().toLowerCase() /* Comment with parens: (). */;",
            "    this.toString().toUpperCase();",
            "  }",
            "}")
        .addOutputLines(
            "A.java",
            "import java.util.Locale;",
            "",
            "class A {",
            "  void m() {",
            "    \"a\".toLowerCase(/* Comment with parens: (). */ Locale.ROOT);",
            "    \"b\".toUpperCase(Locale.ROOT);",
            "    \"c\".toLowerCase(Locale.ROOT).toString();",
            "",
            "    toString().toLowerCase(Locale.ROOT);",
            "    toString().toUpperCase /* Comment with parens: (). */(Locale.ROOT);",
            "",
            "    this.toString().toLowerCase(Locale.ROOT) /* Comment with parens: (). */;",
            "    this.toString().toUpperCase(Locale.ROOT);",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void replacementSecondSuggestedFix() {
    refactoringTestHelper
        .setFixChooser(FixChoosers.SECOND)
        .addInputLines(
            "A.java",
            "class A {",
            "  void m() {",
            "    \"a\".toLowerCase();",
            "    \"b\".toUpperCase(/* Comment with parens: (). */ );",
            "    \"c\".toLowerCase().toString();",
            "",
            "    toString().toLowerCase();",
            "    toString().toUpperCase /* Comment with parens: (). */();",
            "",
            "    this.toString().toLowerCase() /* Comment with parens: (). */;",
            "    this.toString().toUpperCase();",
            "  }",
            "}")
        .addOutputLines(
            "A.java",
            "import java.util.Locale;",
            "",
            "class A {",
            "  void m() {",
            "    \"a\".toLowerCase(Locale.getDefault());",
            "    \"b\".toUpperCase(/* Comment with parens: (). */ Locale.getDefault());",
            "    \"c\".toLowerCase(Locale.getDefault()).toString();",
            "",
            "    toString().toLowerCase(Locale.getDefault());",
            "    toString().toUpperCase /* Comment with parens: (). */(Locale.getDefault());",
            "",
            "    this.toString().toLowerCase(Locale.getDefault()) /* Comment with parens: (). */;",
            "    this.toString().toUpperCase(Locale.getDefault());",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void replacementThirdSuggestedFix() {
    refactoringTestHelper
        .setFixChooser(FixChoosers.THIRD)
        .addInputLines(
            "A.java",
            "class A {",
            "  void m() {",
            "    \"a\".toLowerCase();",
            "    \"c\".toLowerCase().toString();",
            "  }",
            "}")
        .addOutputLines(
            "A.java",
            "import com.google.common.base.Ascii;",
            "class A {",
            "  void m() {",
            "    Ascii.toLowerCase(\"a\");",
            "    Ascii.toLowerCase(\"c\").toString();",
            "  }",
            "}")
        .doTest();
  }
}
