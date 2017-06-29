---
title: WaitNotInLoop
summary: Because of spurious wakeups, Object.wait() and Condition.await() must always be called in a loop
layout: bugpattern
category: JDK
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
`Object.wait()` is supposed to block until either another thread invokes the
`Object.notify()` or `Object.notifyAll()` method, or a specified amount of 
time has elapsed.  The various `Condition.await()` methods have similar 
behavior.  However, it is possible for a thread to wake up without either of
those occurring; these are called *spurious wakeups*.

Because of spurious wakeups, `Object.wait()` and `Condition.await()` must 
always be called in a loop.  The correct fix for this varies depending on what
you are trying to do.

## Wait until a condition becomes true 

The incorrect code for this typically looks like:

Thread 1:

```java
synchronized (this) {
  if (!condition) {
    wait();
  }
  doStuffAssumingConditionIsTrue();
}
```

Thread 2:

```java
synchronized (this) {
  condition = true;
  notify();
}
```

If the call to `wait()` unblocks because of a spurious wakeup, then 
`doStuffAssumingConditionIsTrue()` will be called even though `condition`
is still false.  Instead of the `if`, you should use a `while`:

Thread 1:

```java
synchronized (this) {
  while (!condition) {
    wait();
  }
  doStuffAssumingConditionIsTrue();
}
```
 
This ensures that you only proceed to `doStuffAssumingConditionIsTrue()` if
`condition` is true.  Note that the check of the condition variable must be
inside the synchronized block; otherwise you will have a race condition between
checking and setting the condition variable.

## Wait until an event occurs 

The incorrect code for this typically looks like:

Thread 1:

```java
synchronized (this) {
  wait();
  doStuffAfterEvent();
}
```

Thread 2:

```java
// when event occurs
synchronized (this) {
  notify();
}
```

If the call to `wait()` unblocks because of a spurious wakeup, then 
`doStuffAfterEvent()` will be called even though the event has not yet
occurred.  You should rewrite this code so that the occurrence of the
event sets a condition variable as well as calls `notify()`, and the
`wait()` is wrapped in a while loop checking the condition variable.
That is, it should look just like [the previous example]
(#wait_until_a_condition_becomes_true).

## Wait until either a condition becomes true or a timeout occurs

The incorrect code for this typically looks like:

```java
synchronized (this) {
  if (!condition) {
    wait(timeout);
  }
  doStuffAssumingConditionIsTrueOrTimeoutHasOccurred();
}
```

A spurious wakeup could cause this to proceed to
`doStuffAssumingConditionIsTrueOrTimeoutHasOccurred()` even if the condition is
still false and time less than the timeout has elapsed. Instead, you should
write:

```java
synchronized (this) {
  long now = System.currentTimeMillis();
  long deadline = now + timeout;
  while (!condition && now < deadline) {
    wait(deadline - now);
    now = System.currentTimeMillis();
  }
  doStuffAssumingConditionIsTrueOrTimeoutHasOccurred();
}
```

## Wait for a fixed amount of time

First, a warning: This type of waiting/sleeping is often done when the real
intent is to wait until some operation completes, and then proceed.  If that's
what you're trying to do, please consider rewriting your code to use one of
the patterns above.  Otherwise you are depending on system-specific timing
that *will* change when you run on different machines. 

The incorrect code for this typically looks like:

```java
synchronized (this) {
  // Give some time for the foos to bar
  wait(1000);
}
```

A spurious wakeup could cause this not to wait for a full 1000 ms.  Instead,
you should use `Thread.sleep()`, which is not subject to spurious wakeups:

```java
Thread.sleep(1000);
```

## Wait forever

The incorrect code for this typically looks like:

```java
synchronized (this) {
  // wait forever
  wait();
}
```

A spurious wakeup could cause this not to wait forever.  You should wrap the
call to `wait()` in a `while (true)` loop:

```java
synchronized (this) {
  // wait forever
  while (true) {
    wait();
  }
}
```

## More information

See Java Concurrency in Practice section 14.2.2, "Waking up too soon," [the Javadoc for 
`Object.wait()`](http://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#wait-long-),
and the "Implementation Considerations" section in [the Javadoc for `Condition`]
(https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/Condition.html).

## Suppression
Suppress false positives by adding an `@SuppressWarnings("WaitNotInLoop")` annotation to the enclosing element.

----------

### Positive examples
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

package com.google.errorprone.bugpatterns.testdata;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/** @author eaftan@google.com (Eddie Aftandilian) */
public class WaitNotInLoopPositiveCases {

  boolean flag = false;

  public void testIfInsteadOfLoop() {
    synchronized (this) {
      if (!flag) {
        try {
          // BUG: Diagnostic contains: wait() must always be called in a loop
          // Did you mean 'while (!flag) {'?
          wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public void testWaitLong() throws InterruptedException {
    // BUG: Diagnostic contains: wait(long) must always be called in a loop
    wait(1000);
  }

  public void testWaitLongInt() throws Exception {
    // BUG: Diagnostic contains: wait(long,int) must always be called in a loop
    wait(1000, 1000);
  }

  public void testAwait(Condition cond) throws Exception {
    // BUG: Diagnostic contains: await() must always be called in a loop
    cond.await();
  }

  public void testAwaitWithFix(Condition cond) throws Exception {
    synchronized (this) {
      if (!flag) {
        try {
          // BUG: Diagnostic contains: await() must always be called in a loop
          // Did you mean 'while (!flag) {'?
          cond.await();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public void testAwaitLongTimeUnit(Condition cond) throws Exception {
    // BUG: Diagnostic contains:
    // await(long,java.util.concurrent.TimeUnit) must always be called in a loop
    cond.await(1, TimeUnit.SECONDS);
  }

  public void testAwaitNanos(Condition cond) throws Exception {
    // BUG: Diagnostic contains: awaitNanos(long) must always be called in a loop
    cond.awaitNanos(1000000);
  }

  public void testAwaitUninterruptibly(Condition cond) throws Exception {
    // BUG: Diagnostic contains: awaitUninterruptibly() must always be called in a loop
    cond.awaitUninterruptibly();
  }

  public void testAwaitUntil(Condition cond) throws Exception {
    // BUG: Diagnostic contains: awaitUntil(java.util.Date) must always be called in a loop
    cond.awaitUntil(new Date());
  }
}
{% endhighlight %}

### Negative examples
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

package com.google.errorprone.bugpatterns.testdata;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 *     <p>TODO(eaftan): Add test cases for enhanced for loop, loop outside synchronized block.
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

  // This code is incorrect, but this check should not flag it.
  public void testLoopNotInSynchronized() {
    while (!flag) {
      synchronized (this) {
        try {
          wait();
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
      for (; !flag; ) {
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

  private void wait(Object obj) {}

  public void testNotObjectWait() {
    wait(new Object());
  }
}
{% endhighlight %}

