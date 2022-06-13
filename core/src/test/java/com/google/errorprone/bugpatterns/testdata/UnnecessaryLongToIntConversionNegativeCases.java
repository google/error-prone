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

/** Negative cases for {@link com.google.errorprone.bugpatterns.UnnecessaryLongToIntConversion}. */
public class UnnecessaryLongToIntConversionNegativeCases {

  static void acceptsLong(long value) {}

  static void acceptsInt(int value) {}

  static void acceptsMultipleParams(int intValue, long longValue) {}

  // Converting from a long or Long to an Integer type requires first converting to an int. This is
  // out of scope.
  public void longToIntegerForLongParam() {
    long x = 1;
    acceptsLong(Integer.valueOf((int) x));
  }

  public void longObjectToIntegerForLongParam() {
    Long x = Long.valueOf(1);
    acceptsLong(Integer.valueOf(x.intValue()));
  }

  public void longParameterAndLongArgument() {
    long x = 1;
    acceptsLong(x);
  }

  public void longParameterAndIntArgument() {
    int i = 1;
    acceptsLong(i);
  }

  public void longParameterAndIntegerArgument() {
    Integer i = Integer.valueOf(1);
    acceptsLong(i);
  }

  public void castIntToLong() {
    int i = 1;
    acceptsLong((long) i);
  }

  public void castLongToIntForIntParameter() {
    long x = 1;
    acceptsInt((int) x);
  }

  public void longValueOfLongObject() {
    Long x = Long.valueOf(1);
    acceptsLong(x.longValue());
  }

  public void longValueOfInteger() {
    Integer i = Integer.valueOf(1);
    acceptsLong(i.longValue());
  }

  public void intValueOfInteger() {
    Integer i = Integer.valueOf(1);
    acceptsLong(i.intValue());
  }

  public void intValueForIntParameter() {
    Long x = Long.valueOf(1);
    acceptsInt(x.intValue());
  }

  public void checkedCastOnInt() {
    int i = 1;
    acceptsLong(Ints.checkedCast(i));
  }

  public void checkedCastOnInteger() {
    Integer i = Integer.valueOf(1);
    acceptsLong(Ints.checkedCast(i));
  }

  public void checkedCastForIntParameter() {
    long x = 1;
    acceptsInt(Ints.checkedCast(x));
  }

  public void checkedCastMultipleArgs() {
    long x = 1;
    // The method expects an int for the first parameter and a long for the second paremeter.
    acceptsMultipleParams(Ints.checkedCast(x), x);
  }

  public void toIntExactOnInt() {
    int i = 1;
    acceptsLong(Math.toIntExact(i));
  }

  public void toIntExactOnInteger() {
    Integer i = Integer.valueOf(1);
    acceptsLong(Math.toIntExact(i));
  }

  public void toIntExactForIntParameter() {
    long x = 1;
    acceptsInt(Math.toIntExact(x));
  }
}
