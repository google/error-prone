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
public class MalformedFormatStringPositiveCases {
  private static final Formatter formatter = new Formatter();
  private static final Locale locale = Locale.US;

  public void extraArgs() {
    //BUG: Suggestion includes "System.out.printf("foo");"
    System.out.printf("foo", "bar");
    //BUG: Suggestion includes "formatter.format("%d", 42);"
    formatter.format("%d", 42, 17);
    //BUG: Suggestion includes "String.format(locale, "%n %%");"
    String.format(locale, "%n %%", 1);

    // format call inside other statement
    //BUG: Suggestion includes "throw new Exception(String.format(""));"
    throw new Exception(String.format("", 42));
  }
}
