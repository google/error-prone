/*
 * Copyright 2022 The Error Prone Authors.
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

import com.google.common.primitives.Ints;

/** Positive cases for {@link com.google.errorprone.bugpatterns.UnnecessaryLongToIntConversion}. */
public class UnnecessaryLongToIntConversionPositiveCases {

  static void acceptsLong(long value) {}

  static void acceptsMultipleParams(int intValue, long longValue) {}

  public void longToIntForLongParam() {
    long x = 1;
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong((int) x);
  }

  public void longObjectToIntForLongParam() {
    Long x = Long.valueOf(1);
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong(x.intValue());
  }

  public void convertMultipleArgs() {
    long x = 1;
    // The method expects an int for the first parameter and a long for the second paremeter.
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsMultipleParams(Ints.checkedCast(x), Ints.checkedCast(x));
  }

  // The following test cases test various conversion methods, including an unchecked cast.
  public void castToInt() {
    long x = 1;
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong((int) x);
  }

  public void checkedCast() {
    long x = 1;
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong(Ints.checkedCast(x));
  }

  public void toIntExact() {
    long x = 1;
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong(Math.toIntExact(x));
  }

  public void toIntExactWithLongObject() {
    Long x = Long.valueOf(1);
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong(Math.toIntExact(x));
  }

  public void intValue() {
    Long x = Long.valueOf(1);
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong(x.intValue());
  }
}
