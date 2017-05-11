/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

/** Positive test cases for {@code NonAtomicVolatileUpdate} checker. */
public class NonAtomicVolatileUpdateNegativeCases {

  private volatile int myVolatileInt = 0;
  private int myInt = 0;
  private volatile String myVolatileString = "";
  private String myString = "";

  public void incrementNonVolatile() {
    myInt++;
    ++myInt;
    myInt += 1;
    myInt = myInt + 1;
    myInt = 1 + myInt;

    myInt = myVolatileInt + 1;
    myVolatileInt = myInt + 1;

    myString += "update";
    myString = myString + "update";
  }

  public void decrementNonVolatile() {
    myInt--;
    --myInt;
    myInt -= 1;
    myInt = myInt - 1;
  }

  public synchronized void synchronizedIncrement() {
    myVolatileInt++;
    ++myVolatileInt;
    myVolatileInt += 1;
    myVolatileInt = myVolatileInt + 1;
    myVolatileInt = 1 + myVolatileInt;

    myVolatileString += "update";
    myVolatileString = myVolatileString + "update";
  }

  public void synchronizedBlock() {
    synchronized (this) {
      myVolatileInt++;
      ++myVolatileInt;
      myVolatileInt += 1;
      myVolatileInt = myVolatileInt + 1;
      myVolatileInt = 1 + myVolatileInt;

      myVolatileString += "update";
      myVolatileString = myVolatileString + "update";
    }
  }
}
