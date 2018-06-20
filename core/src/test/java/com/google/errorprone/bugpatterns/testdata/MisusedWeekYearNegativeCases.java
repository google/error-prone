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
