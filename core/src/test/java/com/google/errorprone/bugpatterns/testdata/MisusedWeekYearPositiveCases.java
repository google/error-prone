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
}
