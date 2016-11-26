Protecting writes to a static field by synchronizing on an instance lock is not
thread-safe.

In the following example, two difference instances of `Test` can each acquire
their own instance lock and call `initialize()` at the same time.

```java
class Test {
  private final Object lock = new Object();
  static initialized = false;
  static void initialize() { /* ... */ }

  Test() {
    synchronized (lock) {
      if (!initialized) {
        initialize();
        // error: modification of static variable guarded by instance variable 'lock'
        initialized = true;
      }
      // ...
    }
  }
}
```

Static fields should generally be guarded by static locks, and instance fields
guarded by instance locks.

The example above could be made thread-safe by locking on the enclosing `Class`:

```java
synchronized (Test.class) {
  if (!initialized) {
    initialize();
    initialized = true;
  }
}
```

To update a static counter from an instance method, consider using
[`AtomicInteger`]
(https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/AtomicInteger.html)
instead of incrementing a static `int` field.
