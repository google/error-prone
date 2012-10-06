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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ReturnValueIgnoredPositiveCase1 {
  String a = "thing";
  { // String methods
    //BUG: Suggestion includes "ReturnValueIgnored"
    a.intern();
    //BUG: Suggestion includes "ReturnValueIgnored"
    a.trim();
    //BUG: Suggestion includes "ReturnValueIgnored"
    a.trim().concat("b");
    //BUG: Suggestion includes "ReturnValueIgnored"
    a.concat("append this");
    //BUG: Suggestion includes "ReturnValueIgnored"
    a.replace('t', 'b');
    //BUG: Suggestion includes "ReturnValueIgnored"
    a.replace("thi", "fli");
    //BUG: Suggestion includes "ReturnValueIgnored"
    a.replaceAll("i", "b");
    //BUG: Suggestion includes "ReturnValueIgnored"
    a.replaceFirst("a", "b");
    //BUG: Suggestion includes "ReturnValueIgnored"
    a.toLowerCase();
    //BUG: Suggestion includes "ReturnValueIgnored"
    a.toLowerCase(Locale.ENGLISH);
    //BUG: Suggestion includes "ReturnValueIgnored"
    a.toUpperCase();
    //BUG: Suggestion includes "ReturnValueIgnored"
    a.toUpperCase(Locale.ENGLISH);
    //BUG: Suggestion includes "ReturnValueIgnored"
    a.substring(0);
    //BUG: Suggestion includes "ReturnValueIgnored"
    a.substring(0, 1);
  }

  BigInteger b = new BigInteger("123456789");
  { // BigInteger methods
    //BUG: Suggestion includes "ReturnValueIgnored"
    b.add(new BigInteger("3"));
    //BUG: Suggestion includes "ReturnValueIgnored"
    b.abs();
    //BUG: Suggestion includes "ReturnValueIgnored"
    b.shiftLeft(3);
    //BUG: Suggestion includes "ReturnValueIgnored"
    b.subtract(BigInteger.TEN);
  }

  BigDecimal c = new BigDecimal("1234.5678");
  { // BigDecimal methods
    //BUG: Suggestion includes "ReturnValueIgnored"
    c.add(new BigDecimal("1.3"));
    //BUG: Suggestion includes "ReturnValueIgnored"
    c.abs();
    //BUG: Suggestion includes "ReturnValueIgnored"
    c.divide(new BigDecimal("4.5"));
  }
}
