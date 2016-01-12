---
title: MisusedWeekYear
summary: Use of "YYYY" (week year) in a date pattern without "ww" (week in year). You probably meant to use "yyyy" (year) instead.
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
"YYYY" in a date pattern means "week year".  The week year is defined to begin at the beginning of the week that contains the year's first Thursday.  For example, the week year 2015 began on Monday, December 29, 2014, since January 1, 2015, was on a Thursday.

"Week year" is intended to be used for week dates, e.g. "2015-W01-1", but is often mistakenly used for calendar dates, e.g. 2014-12-29, in which case the year may be incorrect during the last week of the year.  If you are formatting anything other than a week date, you should use the year specifier "yyyy" instead.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MisusedWeekYear")` annotation to the enclosing element.

----------

### Positive examples
__MisusedWeekYearPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MisusedWeekYearPositiveCases {
  void testConstructorWithLiteralPattern() {
    // BUG: Diagnostic contains: new SimpleDateFormat("yyyy-MM-dd")
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd");

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

  void testConstructorWithConstantPattern() {
    // BUG: Diagnostic contains:
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WEEK_YEAR_PATTERN);
  }

  void testConstructorWithConstantPatternWithFolding() {
    // BUG: Diagnostic contains:
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WEEK_YEAR_PATTERN + "-MM-dd");
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
{% endhighlight %}

__MisusedWeekYearPositiveCases2.java__

{% highlight java %}
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

package com.google.errorprone.bugpatterns;

import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.ULocale;

import java.util.Locale;

/**
 * Tests for {@link com.ibm.icu.text.SimpleDateFormat}.
 */
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
{% endhighlight %}

### Negative examples
__MisusedWeekYearNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
{% endhighlight %}

