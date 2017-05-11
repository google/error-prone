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

/** @author ptoomey@google.com (Patrick Toomey) */
public class StringEqualityNegativeCases {

  public boolean testEquality(String x, String y) {
    boolean retVal;

    retVal = x.equals(y);
    retVal = (x == null);
    retVal = (x != null);
    retVal = (null == x);
    retVal = (null != x);

    return retVal;
  }

  @SuppressWarnings("StringEquality")
  public boolean testSuppressWarnings(String x, String y) {
    boolean retVal;

    retVal = (x != y);
    retVal = (x == y);

    return retVal;
  }
}
