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

/** @author Bill Pugh (bill.pugh@gmail.com) */
public class BadShiftAmountPositiveCases {

  public void foo() {
    int x = 0;
    long result = 0;

    // BUG: Diagnostic contains: (long) x >> 32
    result += x >> 32;
    // BUG: Diagnostic contains: (long) x << 32
    result += x << 32;
    // BUG: Diagnostic contains: (long) x >>> 32
    result += x >>> 32;
    // BUG: Diagnostic contains: (long) x >> 40
    result += x >> 40;
    // BUG: Diagnostic contains: (long) (x & 255) >> 40
    result += (x & 255) >> 40;

    // BUG: Diagnostic contains: 1L << 48
    result += 1 << 48;

    // BUG: Diagnostic contains: x >> 4
    result += x >> 100;
    // BUG: Diagnostic contains: x >> 31
    result += x >> -1;

    byte b = 0;
    char c = 'a';

    // BUG: Diagnostic contains: (long) b >> 32
    result += b >> 32;
    // BUG: Diagnostic contains: (long) b << 32
    result += b << 32;
    // BUG: Diagnostic contains: (long) c >> 32
    result += c >> 32;
    // BUG: Diagnostic contains: (long) c >>> 32
    result += c >>> 32;
  }
}
