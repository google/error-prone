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

package com.google.errorprone.bugpatterns.returnvalueignored;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class PositiveCases {
  String a = "thing";
  { // String methods
    a.intern(); //BUG
    a.trim(); //BUG
    a.trim().concat("b"); //BUG
    a.concat("append this"); //BUG
    a.replace('t', 'b'); //BUG
    a.replace("thi", "fli"); //BUG
    a.replaceAll("i", "b"); //BUG
    a.replaceFirst("a", "b"); //BUG
    a.toLowerCase(); //BUG
    a.toLowerCase(Locale.ENGLISH); //BUG
    a.toUpperCase(); //BUG
    a.toUpperCase(Locale.ENGLISH); //BUG
    a.substring(0); //BUG
    a.substring(0, 1); //BUG
    a.split("b"); //BUG
    a.split("b", 1); //BUG
  }

  BigInteger b = new BigInteger("123456789");
  { // BigInteger methods
    b.add(new BigInteger("3")); //BUG
    b.abs(); //BUG
    b.shiftLeft(3); //BUG
    b.subtract(BigInteger.TEN); //BUG
  }

  BigDecimal c = new BigDecimal("1234.5678");
  { // BigDecimal methods
    c.add(new BigDecimal("1.3")); //BUG
    c.abs(); //BUG
    c.divide(new BigDecimal("4.5")); //BUG
  }
}
