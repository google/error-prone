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

/**
 * @author scottjohnson@google.com (Scott Johnson)
 */
public class SelfEqualityNegativeCases {

  public boolean testEquality(int x, int y) {
    boolean retVal;

    int z = 1000;
    retVal = x == y;
    retVal = z == 1000;
    retVal = x != z;
    retVal = x != y;

    return retVal;
  }

  @SuppressWarnings("SelfEquality")
  public boolean testSuppressWarnings(int x) {
    boolean retVal;

    retVal = (x != x);
    retVal = (x == x);

    return retVal;
  }

}
