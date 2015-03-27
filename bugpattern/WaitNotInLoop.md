---
title: WaitNotInLoop
layout: bugpattern
category: JDK
severity: WARNING
maturity: MATURE
---

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>WARNING</td></tr>
<tr><td>Maturity</td><td>MATURE</td></tr>
</table></div>

# Bug pattern: WaitNotInLoop
__Object.wait() should always be called in a loop__

## The problem
Object.wait() can be woken up in multiple ways, none of which guarantee that the condition it was waiting for has become true (spurious wakeups, for example). Thus, Object.wait() should always be called in a loop that checks the condition predicate.  Additionally, the loop should be inside a synchronized block or method to avoid race conditions on the condition predicate.

See Java Concurrency in Practice section 14.2.2, "Waking up too soon," and [http://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#wait() the Javadoc for Object.wait()].

## Suppression
Suppress false positives by adding an `@SuppressWarnings("WaitNotInLoop")` annotation to the enclosing element.

----------

# Examples
__WaitNotInLoopNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
 * @author eaftan@google.com (Eddie Aftandilian)
 *
 * TODO(user): Add test cases for enhanced for loop, loop outside synchronized block.
 */
public class WaitNotInLoopNegativeCases {

  boolean flag = true;

  public void test1() {
    synchronized (this) {
      while (!flag) {
        try {
          wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public void test2() {
    synchronized (this) {
      while (!flag) {
        try {
          wait(1000);
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public void test3() {
    synchronized (this) {
      while (!flag) {
        try {
          wait(1000, 1000);
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public void testDoLoop() {
    synchronized (this) {
      do {
        try {
          wait();
        } catch (InterruptedException e) {
        }
      } while (!flag);
    }
  }

  public void testForLoop() {
    synchronized (this) {
      for (;!flag;) {
        try {
          wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public void testEnhancedForLoop() {
    int[] arr = new int[100];
    synchronized (this) {
      for (int i : arr) {
        try {
          wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  private void wait(Object obj) {
  }

  public void testNotObjectWait() {
    wait(new Object());
  }

}

{% endhighlight %}

__WaitNotInLoopPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class WaitNotInLoopPositiveCases {
  
  boolean flag = true;
  
  public void test1() {
    synchronized (this) {
      if (!flag) {
        try {
          // BUG: Diagnostic contains: 
          wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }
  
  public void test2() {
    synchronized (this) {
      if (!flag) {
        try {
          // BUG: Diagnostic contains: 
          wait(1000);
        } catch (InterruptedException e) {
        }
      }
    }
  }
  
  public void test3() {
    synchronized (this) {
      if (!flag) {
        try {
          // BUG: Diagnostic contains: 
          wait(1000, 1000);
        } catch (InterruptedException e) {
        }
      }
    }
  }
  
  public void testLoopNotInSynchronized() {
    while (!flag) {
      synchronized (this) {
        System.out.println("foo");
        try {
          // BUG: Diagnostic contains: 
          wait(1000, 1000);
        } catch (InterruptedException e) {
        }
      }
    }
  }

}

{% endhighlight %}

