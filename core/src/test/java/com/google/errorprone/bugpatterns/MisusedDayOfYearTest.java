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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link MisusedDayOfYear}. */
@RunWith(JUnit4.class)
public final class MisusedDayOfYearTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(MisusedDayOfYear.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new MisusedDayOfYear(), getClass());

  @Test
  public void positive() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.time.format.DateTimeFormatter;",
            "class Test {",
            "  static {",
            "    DateTimeFormatter.ofPattern(\"yy-MM-DD\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.time.format.DateTimeFormatter;",
            "class Test {",
            "  static {",
            "    DateTimeFormatter.ofPattern(\"yy-MM-dd\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void noDayOfYear_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.time.format.DateTimeFormatter;",
            "class Test {",
            "  static {",
            "    DateTimeFormatter.ofPattern(\"yy-MM-dd\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void noMonthOfYear_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.time.format.DateTimeFormatter;",
            "class Test {",
            "  static {",
            "    DateTimeFormatter.ofPattern(\"yy-DD\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void escapedInQuotes() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.time.format.DateTimeFormatter;",
            "class Test {",
            "  static {",
            "    DateTimeFormatter.ofPattern(\"'D'yy-MM-dd\");",
            "    DateTimeFormatter.ofPattern(\"'D'''yy-MM-dd\");",
            "    DateTimeFormatter.ofPattern(\"'''D'yy-MM-dd\");",
            "    DateTimeFormatter.ofPattern(\"'D''D'yy-MM-dd\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void escapedInQuotes_butAlsoAsSpecialChar() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.time.format.DateTimeFormatter;",
            "class Test {",
            "  static {",
            "    DateTimeFormatter.ofPattern(\"'D'yy-MM-DD\");",
            "    DateTimeFormatter.ofPattern(\"'D'''yy-MM-DD\");",
            "    DateTimeFormatter.ofPattern(\"'''D'yy-MM-DD\");",
            "    DateTimeFormatter.ofPattern(\"'D''D'yy-MM-DD\");",
            "    DateTimeFormatter.ofPattern(\"'M'yy-DD\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.time.format.DateTimeFormatter;",
            "class Test {",
            "  static {",
            "    DateTimeFormatter.ofPattern(\"'D'yy-MM-dd\");",
            "    DateTimeFormatter.ofPattern(\"'D'''yy-MM-dd\");",
            "    DateTimeFormatter.ofPattern(\"'''D'yy-MM-dd\");",
            "    DateTimeFormatter.ofPattern(\"'D''D'yy-MM-dd\");",
            "    DateTimeFormatter.ofPattern(\"'M'yy-DD\");",
            "  }",
            "}")
        .doTest();
  }
}
