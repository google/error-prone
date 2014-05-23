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

package com.google.errorprone.bugpatterns;

public class IncrementDecrementVolatilePositiveCases {
  
  private static class VolatileContainer {
    public volatile int volatileInt = 0;
  }
  
  private volatile int myVolatileInt = 0;
  private VolatileContainer container = new VolatileContainer();
 
  public void increment() {
    //BUG: Suggestion includes ""
    myVolatileInt++;
    //BUG: Suggestion includes ""
    ++myVolatileInt;
    //BUG: Suggestion includes ""
    myVolatileInt += 1;
    //BUG: Suggestion includes ""
    myVolatileInt = myVolatileInt + 1;
    //BUG: Suggestion includes ""
    myVolatileInt = 1 + myVolatileInt;
    
    //BUG: Suggestion includes ""
    if (myVolatileInt++ == 0) {
      System.out.println("argh");
    }

    
    //BUG: Suggestion includes ""
    container.volatileInt++;
    //BUG: Suggestion includes ""
    ++container.volatileInt;
    //BUG: Suggestion includes ""
    container.volatileInt += 1;
    //BUG: Suggestion includes ""
    container.volatileInt = container.volatileInt + 1;
    //BUG: Suggestion includes ""
    container.volatileInt = 1 + container.volatileInt;
  }
  
  public void decrement() {
    //BUG: Suggestion includes ""
    myVolatileInt--;
    //BUG: Suggestion includes ""
    --myVolatileInt;
    //BUG: Suggestion includes ""
    myVolatileInt -= 1;
    //BUG: Suggestion includes ""
    myVolatileInt = myVolatileInt - 1;
    
    //BUG: Suggestion includes ""
    container.volatileInt--;
    //BUG: Suggestion includes ""
    --container.volatileInt;
    //BUG: Suggestion includes ""
    container.volatileInt -= 1;
    //BUG: Suggestion includes ""
    container.volatileInt = container.volatileInt - 1;
  }  
}

