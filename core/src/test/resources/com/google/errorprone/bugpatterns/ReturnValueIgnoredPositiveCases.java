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
public class ReturnValueIgnoredPositiveCases {
  String a = "thing";
  { // String methods
    a.intern(); //BUG("ReturnValueIgnored")
    a.trim(); //BUG("ReturnValueIgnored")
    a.trim().concat("b"); //BUG("ReturnValueIgnored")
    a.concat("append this"); //BUG("ReturnValueIgnored")
    a.replace('t', 'b'); //BUG("ReturnValueIgnored")
    a.replace("thi", "fli"); //BUG("ReturnValueIgnored")
    a.replaceAll("i", "b"); //BUG("ReturnValueIgnored")
    a.replaceFirst("a", "b"); //BUG("ReturnValueIgnored")
    a.toLowerCase(); //BUG("ReturnValueIgnored")
    a.toLowerCase(Locale.ENGLISH); //BUG("ReturnValueIgnored")
    a.toUpperCase(); //BUG("ReturnValueIgnored")
    a.toUpperCase(Locale.ENGLISH); //BUG("ReturnValueIgnored")
    a.substring(0); //BUG("ReturnValueIgnored")
    a.substring(0, 1); //BUG("ReturnValueIgnored")
    a.split("b"); //BUG("ReturnValueIgnored")
    a.split("b", 1); //BUG("ReturnValueIgnored")
  }

  BigInteger b = new BigInteger("123456789");
  { // BigInteger methods
    b.add(new BigInteger("3")); //BUG("ReturnValueIgnored")
    b.abs(); //BUG("ReturnValueIgnored")
    b.shiftLeft(3); //BUG("ReturnValueIgnored")
    b.subtract(BigInteger.TEN); //BUG("ReturnValueIgnored")
  }

  BigDecimal c = new BigDecimal("1234.5678");
  { // BigDecimal methods
    c.add(new BigDecimal("1.3")); //BUG("ReturnValueIgnored")
    c.abs(); //BUG("ReturnValueIgnored")
    c.divide(new BigDecimal("4.5")); //BUG("ReturnValueIgnored")
  }
}
