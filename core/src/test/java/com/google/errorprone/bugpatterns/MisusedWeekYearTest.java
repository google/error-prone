/*
 * Copyright 2015 The Error Prone Authors.
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

/** Test cases for {@link MisusedWeekYear}. */
@RunWith(JUnit4.class)
public class MisusedWeekYearTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MisusedWeekYear.class, getClass());

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "MisusedWeekYearPositiveCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MisusedWeekYearPositiveCases {
  void testConstructorWithLiteralPattern() {
    // BUG: Diagnostic contains: new SimpleDateFormat("yyyy-MM-dd")
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd");

    // BUG: Diagnostic contains: new SimpleDateFormat("yy-MM-dd")
    simpleDateFormat = new SimpleDateFormat("YY-MM-dd");

    // BUG: Diagnostic contains: new SimpleDateFormat("y-MM-dd")
    simpleDateFormat = new SimpleDateFormat("Y-MM-dd");

    // BUG: Diagnostic contains: new SimpleDateFormat("yyyyMMdd_HHmm")
    simpleDateFormat = new SimpleDateFormat("YYYYMMdd_HHmm");

    // BUG: Diagnostic contains: new SimpleDateFormat("yyyy-MM-dd", DateFormatSymbols.getInstance())
    simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd", DateFormatSymbols.getInstance());

    // BUG: Diagnostic contains: new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd", Locale.getDefault());
  }

  void testConstructorWithLiteralPatternWithFolding() {
    // TODO(eaftan): javac has a bug in that when it folds string literals, the start position of
    // the folded string literal node is set as the start position of the + operator.  We have
    // fixed this in our internal javac, but haven't pushed the change to our external javac mirror.
    // We should push that fix to the javac mirror repo, and then we can test that the suggested
    // fix offered here is correct ("yyyy-MM-dd").
    // BUG: Diagnostic contains:
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY" + "-MM-dd");
  }

  private static final String WEEK_YEAR_PATTERN = "YYYY";

  private static final String WEEK_YEAR_PATTERN_2 = "YY";

  private static final String WEEK_YEAR_PATTERN_3 = "Y";

  void testConstructorWithConstantPattern() {
    // BUG: Diagnostic contains:
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WEEK_YEAR_PATTERN);
  }

  void testConstructorWithConstantPattern2() {
    // BUG: Diagnostic contains:
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WEEK_YEAR_PATTERN_2);
  }

  void testConstructorWithConstantPattern3() {
    // BUG: Diagnostic contains:
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WEEK_YEAR_PATTERN_3);
  }

  void testConstructorWithConstantPatternWithFolding() {
    // BUG: Diagnostic contains:
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WEEK_YEAR_PATTERN + "-MM-dd");
  }

  void testConstructorWithConstantPatternWithFolding2() {
    // BUG: Diagnostic contains:
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WEEK_YEAR_PATTERN_2 + "-MM-dd");
  }

  void testConstructorWithConstantPatternWithFolding3() {
    // BUG: Diagnostic contains:
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WEEK_YEAR_PATTERN_3 + "-MM-dd");
  }

  void testApplyPatternAndApplyLocalizedPatternWithLiteralPattern() {
    SimpleDateFormat sdf = new SimpleDateFormat();
    // BUG: Diagnostic contains: sdf.applyPattern("yyyy-MM-dd")
    sdf.applyPattern("YYYY-MM-dd");
    // BUG: Diagnostic contains: sdf.applyLocalizedPattern("yyyy-MM-dd")
    sdf.applyLocalizedPattern("YYYY-MM-dd");
  }

  void testApplyPatternAndApplyLocalizedPatternWithConstantPattern() {
    SimpleDateFormat sdf = new SimpleDateFormat();
    // BUG: Diagnostic contains:
    sdf.applyPattern(WEEK_YEAR_PATTERN);
    // BUG: Diagnostic contains:
    sdf.applyLocalizedPattern(WEEK_YEAR_PATTERN);
  }

  void testDateTimeFormatter() {
    // BUG: Diagnostic contains:
    java.time.format.DateTimeFormatter.ofPattern(WEEK_YEAR_PATTERN);
  }
}
""")
        .doTest();
  }

  @Test
  public void positiveCases2() {
    compilationHelper
        .addSourceLines(
            "MisusedWeekYearPositiveCases2.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.ULocale;
import java.util.Locale;

/** Tests for {@link com.ibm.icu.text.SimpleDateFormat}. */
public class MisusedWeekYearPositiveCases2 {

  void testConstructors() {
    // BUG: Diagnostic contains: new SimpleDateFormat("yyyy-MM-dd")
    SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd");

    // BUG: Diagnostic contains:
    sdf = new SimpleDateFormat("YYYY-MM-dd", DateFormatSymbols.getInstance());

    // BUG: Diagnostic contains:
    sdf = new SimpleDateFormat("YYYY-MM-dd", DateFormatSymbols.getInstance(), ULocale.CANADA);

    // BUG: Diagnostic contains:
    sdf = new SimpleDateFormat("YYYY-MM-dd", Locale.getDefault());

    // BUG: Diagnostic contains:
    sdf = new SimpleDateFormat("YYYY-MM-dd", "", ULocale.CANADA);

    // BUG: Diagnostic contains:
    sdf = new SimpleDateFormat("YYYY-MM-dd", ULocale.CANADA);
  }

  void testApplyPatternAndApplyLocalizedPattern() {
    SimpleDateFormat sdf = new SimpleDateFormat();
    // BUG: Diagnostic contains: sdf.applyPattern("yyyy-MM-dd")
    sdf.applyPattern("YYYY-MM-dd");
    // BUG: Diagnostic contains:
    sdf.applyLocalizedPattern("YYYY-MM-dd");
  }
}
""")
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "MisusedWeekYearNegativeCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MisusedWeekYearNegativeCases {
  void testLiteralPattern() {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    simpleDateFormat = new SimpleDateFormat("MM-dd");
    simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", DateFormatSymbols.getInstance());
    simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Don't flag if the pattern contains "ww", the week-in-year specifier.
    simpleDateFormat = new SimpleDateFormat("YYYY-ww");
    simpleDateFormat = new SimpleDateFormat("YY-ww");
    simpleDateFormat = new SimpleDateFormat("Y-ww");
    simpleDateFormat = new SimpleDateFormat("Yw");
  }

  void testLiteralPatternWithFolding() {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy" + "-MM-dd");
  }

  private static final String WEEK_YEAR_PATTERN = "yyyy-MM-dd";

  void testConstantPattern() {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WEEK_YEAR_PATTERN);
  }

  private static class MySimpleDateFormat extends SimpleDateFormat {
    public MySimpleDateFormat(String pattern) {
      super(pattern);
    }
  }

  // Don't match on subtypes, since we don't know what their applyPattern and
  // applyLocalizedPattern methods might do.
  void testSubtype() {
    MySimpleDateFormat mySdf = new MySimpleDateFormat("YYYY-MM-dd");
    mySdf.applyPattern("YYYY-MM-dd");
    mySdf.applyLocalizedPattern("YYYY-MM-dd");
  }
}
""")
        .doTest();
  }

  @Test
  public void refactoring() {
    BugCheckerRefactoringTestHelper.newInstance(MisusedWeekYear.class, getClass())
        .addInputLines(
            "Test.java",
            """
            import java.time.format.DateTimeFormatter;

            class Test {
              private static final String PATTERN = "YYYY";
              static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.time.format.DateTimeFormatter;

            class Test {
              private static final String PATTERN = "yyyy";
              static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);
            }
            """)
        .doTest();
  }
}
