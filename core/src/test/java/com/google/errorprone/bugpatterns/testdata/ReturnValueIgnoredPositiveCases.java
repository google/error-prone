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

package com.google.errorprone.bugpatterns;

import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Locale;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ReturnValueIgnoredPositiveCases {
  String a = "thing";
  { // String methods
    // BUG: Diagnostic contains: remove this line
    String.format("%d", 10);
    // BUG: Diagnostic contains: remove this line
    String.format("%d", 10).trim();
    // BUG: Diagnostic contains: remove this line
    java.lang.String.format("%d", 10).trim();
    // BUG: Diagnostic contains: a = a.intern()
    a.intern();
    // BUG: Diagnostic contains: a = a.trim()
    a.trim();
    // BUG: Diagnostic contains: a = a.trim().concat("b")
    a.trim().concat("b");
    // BUG: Diagnostic contains: a = a.concat("append this")
    a.concat("append this");
    // BUG: Diagnostic contains: a = a.replace('t', 'b')
    a.replace('t', 'b');
    // BUG: Diagnostic contains: a = a.replace("thi", "fli")
    a.replace("thi", "fli");
    // BUG: Diagnostic contains: a = a.replaceAll("i", "b")
    a.replaceAll("i", "b");
    // BUG: Diagnostic contains: a = a.replaceFirst("a", "b")
    a.replaceFirst("a", "b");
    // BUG: Diagnostic contains: a = a.toLowerCase()
    a.toLowerCase();
    // BUG: Diagnostic contains: a = a.toLowerCase(Locale.ENGLISH)
    a.toLowerCase(Locale.ENGLISH);
    // BUG: Diagnostic contains: a = a.toUpperCase()
    a.toUpperCase();
    // BUG: Diagnostic contains: a = a.toUpperCase(Locale.ENGLISH)
    a.toUpperCase(Locale.ENGLISH);
    // BUG: Diagnostic contains: a = a.substring(0)
    a.substring(0);
    // BUG: Diagnostic contains: a = a.substring(0, 1)
    a.substring(0, 1);
  }
  
  StringBuffer sb = new StringBuffer("hello");
  {
    // BUG: Diagnostic contains: remove this line
    sb.toString().trim();
  }

  BigInteger b = new BigInteger("123456789");
  { // BigInteger methods
    // BUG: Diagnostic contains: b = b.add(new BigInteger("3"))
    b.add(new BigInteger("3"));
    // BUG: Diagnostic contains: b = b.abs()
    b.abs();
    // BUG: Diagnostic contains: b = b.shiftLeft(3)
    b.shiftLeft(3);
    // BUG: Diagnostic contains: b = b.subtract(BigInteger.TEN)
    b.subtract(BigInteger.TEN);
  }

  BigDecimal c = new BigDecimal("1234.5678");
  { // BigDecimal methods
    // BUG: Diagnostic contains: c = c.add(new BigDecimal("1.3"))
    c.add(new BigDecimal("1.3"));
    // BUG: Diagnostic contains: c = c.abs()
    c.abs();
    // BUG: Diagnostic contains: c = c.divide(new BigDecimal("4.5"))
    c.divide(new BigDecimal("4.5"));
    // BUG: Diagnostic contains: remove this line
    new BigDecimal("10").add(c);
  }
  
}
