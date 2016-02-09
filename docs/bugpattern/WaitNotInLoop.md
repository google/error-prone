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
