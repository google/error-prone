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

import java.math.BigDecimal;

/** @author endobson@google.com (Eric Dobson) */
public class BigDecimalLiteralDoublePositiveCases {

  public void foo() {
    // BUG: Diagnostic contains: BigDecimal.ZERO
    new BigDecimal(0.0);

    // BUG: Diagnostic contains: new BigDecimal("1.0")
    BigDecimal.valueOf(1.0);

    // BUG: Diagnostic contains: BigDecimal.ONE
    new BigDecimal(1.0);

    // BUG: Diagnostic contains: BigDecimal.TEN
    new BigDecimal(10.0);

    // BUG: Diagnostic contains: new BigDecimal(99L)
    new BigDecimal(99.0);

    // BUG: Diagnostic contains: new BigDecimal(123456L)
    new BigDecimal(123_456.0);

    // BUG: Diagnostic contains: new BigDecimal(".045")
    BigDecimal.valueOf(.045);

    // BUG: Diagnostic contains: new BigDecimal("123456.0E-4")
    new BigDecimal(123456.0E-4);

    // BUG: Diagnostic contains: new BigDecimal("123456.0")
    BigDecimal.valueOf(123456.0D);

    // BUG: Diagnostic contains: new BigDecimal("-0.012")
    new BigDecimal(-0.012);

    // BUG: Diagnostic contains: new BigDecimal("+.034")
    BigDecimal.valueOf(+.034);
  }
}
