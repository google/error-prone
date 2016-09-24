---
title: NonAtomicVolatileUpdate
summary: This update of a volatile variable is non-atomic
layout: bugpattern
category: JDK
severity: WARNING
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The volatile modifier ensures that updates to a variable are propagated predictably to other threads.  A read of a volatile variable always returns the most recent write by any thread.

However, this does not mean that all updates to a volatile variable are atomic.  For example, if you increment or decrement a volatile variable, you are actually doing (1) a read of the variable, (2) an increment or decrement of a local copy, and (3) a write back to the variable. Each step is atomic individually, but the whole sequence is not, and it will cause a race condition if two threads try to increment or decrement a volatile variable at the same time.  The same is true for compound assignment, e.g. foo += bar.

If you intended for this update to be atomic, you should wrap all update operations on this variable in a synchronized block.  If the variable is an integer, you could use an AtomicInteger instead of a volatile int.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("NonAtomicVolatileUpdate")` annotation to the enclosing element.

----------

### Positive examples
__NonAtomicVolatileUpdatePositiveCases.java__

{% highlight java %}
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

/**
 * Positive test cases for {@code NonAtomicVolatileUpdate} checker.
 */
public class NonAtomicVolatileUpdatePositiveCases {
    
  private static class VolatileContainer {
    public volatile int volatileInt = 0;
  }
  
  private volatile int myVolatileInt = 0;
  private VolatileContainer container = new VolatileContainer();
 
  public void increment() {
    // BUG: Diagnostic contains: 
    myVolatileInt++;
    // BUG: Diagnostic contains: 
    ++myVolatileInt;
    // BUG: Diagnostic contains: 
    myVolatileInt += 1;
    // BUG: Diagnostic contains: 
    myVolatileInt = myVolatileInt + 1;
    // BUG: Diagnostic contains: 
    myVolatileInt = 1 + myVolatileInt;
    
    // BUG: Diagnostic contains: 
    if (myVolatileInt++ == 0) {
      System.out.println("argh");
    }

    
    // BUG: Diagnostic contains: 
    container.volatileInt++;
    // BUG: Diagnostic contains: 
    ++container.volatileInt;
    // BUG: Diagnostic contains: 
    container.volatileInt += 1;
    // BUG: Diagnostic contains: 
    container.volatileInt = container.volatileInt + 1;
    // BUG: Diagnostic contains: 
    container.volatileInt = 1 + container.volatileInt;
  }
  
  public void decrement() {
    // BUG: Diagnostic contains: 
    myVolatileInt--;
    // BUG: Diagnostic contains: 
    --myVolatileInt;
    // BUG: Diagnostic contains: 
    myVolatileInt -= 1;
    // BUG: Diagnostic contains: 
    myVolatileInt = myVolatileInt - 1;
    
    // BUG: Diagnostic contains: 
    container.volatileInt--;
    // BUG: Diagnostic contains: 
    --container.volatileInt;
    // BUG: Diagnostic contains: 
    container.volatileInt -= 1;
    // BUG: Diagnostic contains: 
    container.volatileInt = container.volatileInt - 1;
  }
  
  private volatile String myVolatileString = "";
  
  public void stringUpdate() {
    // BUG: Diagnostic contains:
    myVolatileString += "update";
    // BUG: Diagnostic contains:
    myVolatileString = myVolatileString + "update";
  }
}
{% endhighlight %}

### Negative examples
__NonAtomicVolatileUpdateNegativeCases.java__

{% highlight java %}
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

/**
 * Positive test cases for {@code NonAtomicVolatileUpdate} checker.
 */
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
{% endhighlight %}

