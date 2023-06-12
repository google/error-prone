/*
 * Copyright 2013 The Error Prone Authors.
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

import java.io.IOException;
import java.io.Reader;
import java.util.function.Supplier;

/**
 * @author Bill Pugh (bill.pugh@gmail.com)
 */
class ComparisonOutOfRangePositiveCases {
  private static final int NOT_A_BYTE = 255;

  void byteEquality() {
    boolean result;
    byte b = 0;
    byte[] barr = {1, 2, 3};

    // BUG: Diagnostic contains: b == -1
    result = b == 255;
    // BUG: Diagnostic contains: b == 1
    result = b == -255;
    // BUG: Diagnostic contains: b == -128
    result = b == 128;
    // BUG: Diagnostic contains: b != -1
    result = b != 255;

    // BUG: Diagnostic contains: barr[0] == -1
    result = barr[0] == 255;
    // BUG: Diagnostic contains:
    result = barr[0] == 128;
    // BUG: Diagnostic contains: bytes
    result = barr[0] == -255;

    // BUG: Diagnostic contains: b == -1
    result = b == NOT_A_BYTE;

    Byte boxed = 0;
    // BUG: Diagnostic contains:
    result = boxed == 255;
    Supplier<? extends Byte> bSupplier = null;
    // BUG: Diagnostic contains:
    result = bSupplier.get() == 255;
  }

  void charEquality() throws IOException {
    boolean result;
    char c = 'A';
    Reader reader = null;

    // BUG: Diagnostic contains: false
    result = c == -1;
    // BUG: Diagnostic contains: true
    result = c != -1;

    char d;
    // BUG: Diagnostic contains: chars
    result = (d = (char) reader.read()) == -1;
  }

  void shorts(short s) {
    boolean result;

    // BUG: Diagnostic contains: false
    result = s == Short.MAX_VALUE + 1;
    // BUG: Diagnostic contains: false
    result = s == Short.MIN_VALUE - 1;

    // BUG: Diagnostic contains: true
    result = s != Short.MAX_VALUE + 1;
    // BUG: Diagnostic contains: true
    result = s != Short.MIN_VALUE - 1;

    // BUG: Diagnostic contains: false
    result = s > Short.MAX_VALUE;
    // BUG: Diagnostic contains: true
    result = s > Short.MIN_VALUE - 1;

    // BUG: Diagnostic contains: false
    result = s >= Short.MAX_VALUE + 1;
    // BUG: Diagnostic contains: true
    result = s >= Short.MIN_VALUE;

    // BUG: Diagnostic contains: false
    result = s < Short.MIN_VALUE;
    // BUG: Diagnostic contains: true
    result = s < Short.MAX_VALUE + 1;

    // BUG: Diagnostic contains: false
    result = s <= Short.MIN_VALUE - 1;
    // BUG: Diagnostic contains: true
    result = s <= Short.MAX_VALUE;
  }

  void shortsReversed(short s) {
    boolean result;

    // BUG: Diagnostic contains: false
    result = Short.MAX_VALUE < s;
    // BUG: Diagnostic contains: true
    result = Short.MIN_VALUE - 1 < s;

    // BUG: Diagnostic contains: false
    result = Short.MAX_VALUE + 1 <= s;
    // BUG: Diagnostic contains: true
    result = Short.MIN_VALUE <= s;

    // BUG: Diagnostic contains: false
    result = Short.MIN_VALUE > s;
    // BUG: Diagnostic contains: true
    result = Short.MAX_VALUE + 1 > s;

    // BUG: Diagnostic contains: false
    result = Short.MIN_VALUE - 1 >= s;
    // BUG: Diagnostic contains: true
    result = Short.MAX_VALUE >= s;
  }

  void ints(int i) {
    boolean result;

    // BUG: Diagnostic contains: false
    result = i == Integer.MAX_VALUE + 1L;
  }

  void longs(long l) {
    boolean result;

    // BUG: Diagnostic contains: false
    result = l == Long.MIN_VALUE * 2.0;
  }
}
