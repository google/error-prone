---
title: UnsynchronizedOverridesSynchronized
summary: Unsynchronized method overrides a synchronized method.
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
Thread-safe methods should never be overridden by methods that are not
thread-safe. Doing so violates behavioural subtyping, and can result in bugs if
the subtype is used in contexts that rely on the thread-safety of the supertype.

Overriding a `synchronized` method with a method that is not `synchronized` can
be a sign that the thread-safety of the supertype is not being preserved.

```java
class Counter {
  private int count = 0;

  synchronized void increment() {
    count++;
  }
}
```

```java
// MyCounter is not thread safe!
class MyCounter extends Counter {
  private int count = 0;

  void increment() {
    count++;
  }
}
```

Note that there are many ways to implement a thread-safe method without using
the `synchronized` modifier (e.g. `synchronized` statements using explicit
locks, or other locking constructs). When overriding a `synchronized` method
with a method that is thread-safe but does not have the `synchronized` modifier,
consider adding `@SuppressWarnings("UnsynchronizedOverridesSynchronized")`
and an explanation.

```java
class MyCounter extends Counter {
  private AtomicInteger count = AtomicInteger();

  @SuppressWarnings("UnsynchronizedOverridesSynchronized") // AtomicInteger is thread-safe
  void increment() {
    count.getAndIncrement();
  }
}
```

## Suppression
Suppress false positives by adding an `@SuppressWarnings("UnsynchronizedOverridesSynchronized")` annotation to the enclosing element.
