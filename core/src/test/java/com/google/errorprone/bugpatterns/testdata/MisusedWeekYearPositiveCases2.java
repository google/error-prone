/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
