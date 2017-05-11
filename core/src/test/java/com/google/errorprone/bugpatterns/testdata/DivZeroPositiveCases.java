/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

/** @author cushon@google.com (Liam Miller-Cushon) */
public class DivZeroPositiveCases {

  public static void main(String[] args) {
    new DivZeroPositiveCases();
  }

  static int staticOne = 0;

  // BUG: Diagnostic contains: DivZero
  static int staticTwo = staticOne / 0;

  // BUG: Diagnostic contains: DivZero
  static int staticThree = (staticTwo / 0);

  int fieldOne;

  // BUG: Diagnostic contains: DivZero
  int fieldTwo = fieldOne / 0;

  // BUG: Diagnostic contains: DivZero
  int fieldThree = (fieldTwo /= 0);

  int f() {
    return 42;
  }

  void method(final int a, double b, boolean flag) {
    int x;
    double y;

    // BUG: Diagnostic contains: throw new ArithmeticException
    x = a / 0;

    // BUG: Diagnostic contains: throw new ArithmeticException
    x /= 0;

    x =
        ((((a / a) / (a / a)) / ((a / a) / (a / a))) / (((a / a) / (a / a)) / ((a / a) / (a / a))))
            /
            // BUG: Diagnostic contains: zero
            ((((a / a) / (a / a)) / ((a / 0) / (a / a)))
                / (((a / a) / (a / a)) / ((a / a) / (a / a))));

    // BUG: Diagnostic contains: throw new ArithmeticException
    x = flag ? a / 0 : 42;

    // BUG: Diagnostic contains: throw new ArithmeticException
    for (int i = 0; i < 10; i /= 0) {}

    Object o =
        new Object() {
          // BUG: Diagnostic contains: throw new ArithmeticException
          int x = a / 0;
        };

    // BUG: Diagnostic contains: throw new ArithmeticException
    x = f() / 0;
  }

  // TODO(cushon): write a check for self-references via qualified names in field initializers,
  // even if JLS 8.3.2.3 permits it.

  // BUG: Diagnostic contains: DivZero
  int selfRefField = this.selfRefField / 0;

  // BUG: Diagnostic contains: DivZero
  static int staticSelfRefField = DivZeroPositiveCases.staticSelfRefField / 0;
}
