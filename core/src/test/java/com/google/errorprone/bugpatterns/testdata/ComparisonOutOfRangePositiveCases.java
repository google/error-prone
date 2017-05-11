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

import java.io.IOException;
import java.io.Reader;

/** @author Bill Pugh (bill.pugh@gmail.com) */
public class ComparisonOutOfRangePositiveCases {

  public void testByteEquality() {
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
    // BUG: Diagnostic contains: b == 1
    result = b == -255;

    // BUG: Diagnostic contains: barr[0] == -1
    result = barr[0] == 255;
    // BUG: Diagnostic contains: barr[0] == -128
    result = barr[0] == 128;
    // BUG: Diagnostic contains: barr[0] == 1
    result = barr[0] == -255;
  }

  public void testCharEquality() throws IOException {
    boolean result;
    char c = 'A';
    Reader reader = null;

    // BUG: Diagnostic contains: false
    result = c == -1;
    // BUG: Diagnostic contains: true
    result = c != -1;

    char d;
    // BUG: Diagnostic contains: false
    result = (d = (char) reader.read()) == -1;
  }
}
