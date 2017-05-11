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
public class ComparisonOutOfRangeNegativeCases {

  public void testByteEquality() {
    boolean result;
    byte b = 0;
    byte[] barr = {1, 2, 3};

    result = b == 1;
    result = b == -2;
    result = b == 127;
    result = b != 1;

    result = b == (byte) 255;

    result = b == 'a'; // char
    result = b == 1L; // long
    result = b == 1.123f; // float
    result = b == 1.123; // double

    result = barr[0] == 1;
    result = barr[0] == -2;
    result = barr[0] == -128;
  }

  public void testCharEquality() throws IOException {
    boolean result;
    char c = 'A';
    Reader reader = null;

    result = c == 0;
    result = c == 0xffff;

    result = c == 1L; // long
    result = c == 1.123f; // float
    result = c == 1.123; // double

    int d;
    result = (d = reader.read()) == -1;
  }
}
