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

package com.google.errorprone.bugpatterns.testdata;

/** @author scottjohnson@google.com (Scott Johnson) */
public class NumericEqualityPositiveCases {

  public boolean testIntegers(Integer x, Integer y) {
    boolean retVal;

    // BUG: Diagnostic contains: Objects.equals(x, y)
    retVal = (x == y);

    // BUG: Diagnostic contains: !Objects.equals(x, y)
    retVal = (x != y);
    final Integer constValue = new Integer(1000);

    // BUG: Diagnostic contains: Objects.equals(x, constValue)
    retVal = (x == constValue);

    // BUG: Diagnostic contains: !Objects.equals(x, constValue)
    retVal = (x != constValue);

    return retVal;
  }

  public boolean testLongs(Long x, Long y) {
    boolean retVal;

    // BUG: Diagnostic contains: Objects.equals(x, y)
    retVal = (x == y);

    // BUG: Diagnostic contains: !Objects.equals(x, y)
    retVal = (x != y);
    final Long constValue = new Long(1000L);

    // BUG: Diagnostic contains: Objects.equals(x, constValue)
    retVal = (x == constValue);

    // BUG: Diagnostic contains: !Objects.equals(x, constValue)
    retVal = (x != constValue);

    return retVal;
  }

  public boolean testMixed(Integer x, Number y) {
    boolean retVal;

    // BUG: Diagnostic contains: Objects.equals(x, y)
    retVal = (x == y);

    // BUG: Diagnostic contains: !Objects.equals(x, y)
    retVal = (x != y);
    final Number constValue = new Long(1000L);

    // BUG: Diagnostic contains: Objects.equals(x, constValue)
    retVal = (x == constValue);

    // BUG: Diagnostic contains: !Objects.equals(x, constValue)
    retVal = (x != constValue);

    return retVal;
  }
}
