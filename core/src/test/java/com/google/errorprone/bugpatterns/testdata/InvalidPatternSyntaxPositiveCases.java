/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import com.google.common.base.Splitter;
import java.util.regex.Pattern;

/** @author mdempsky@google.com (Matthew Dempsky) */
public class InvalidPatternSyntaxPositiveCases {
  public static final String INVALID = "*";
  public static final String DOT = ".";

  {
    // BUG: Diagnostic contains: Unclosed character class
    Pattern.matches("[^\\]", "");
    // BUG: Diagnostic contains: Unclosed character class
    Pattern.matches("[a-z", "");
    // BUG: Diagnostic contains: Illegal repetition
    Pattern.matches("{", "");

    // BUG: Diagnostic contains:
    Pattern.matches(INVALID, "");
    // BUG: Diagnostic contains:
    "".matches(INVALID);
    // BUG: Diagnostic contains:
    "".replaceAll(INVALID, "");
    // BUG: Diagnostic contains:
    "".replaceFirst(INVALID, "");
    // BUG: Diagnostic contains:
    "".split(INVALID);
    // BUG: Diagnostic contains:
    "".split(INVALID, 0);

    // BUG: Diagnostic contains: "foo.bar".split("\\.")
    "foo.bar".split(".");
    // BUG: Diagnostic contains:
    "foo.bonk".split(DOT);

    // BUG: Diagnostic contains: Splitter.onPattern("\\.")
    Splitter.onPattern(".");
  }
}
