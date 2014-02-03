/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import java.util.Formatter;
import java.util.Locale;
/**
 * @author rburny@google.com (Radoslaw Burny)
 */
public class MalformedFormatStringNegativeCases {
  private static final Formatter formatter = new Formatter();
  private static final Locale locale = Locale.US;

  public void literals() {
    System.out.printf("nothing here");
    System.out.printf("%d", 42);
    formatter.format("%f %b %s", 3.14, true, "foo");
    String.format("%c %x %s", '$', 7, new Object());
    String.format("%h %o", new Object(), 17);

    System.out.printf(locale, "%d", 42);
    formatter.format(locale, "%d", 42);
    String.format(locale, "%d", 42);
  }

  public void variables() {
    String s = "bar";
    Integer i = 7;
    int j = 8;
    float f = 2.71f;
    Object o = new Object();

    System.out.printf("%d %s %d %f", i, s, j, f);
    formatter.format("%d %f", i, f);
    String.format("%d %o", j, o);
  }

  public void specialCases() {
    System.out.printf("%b", new Object());
    System.err.printf("%s %d %f", null, null, null);
    System.err.printf("%n %%");
  }
}
