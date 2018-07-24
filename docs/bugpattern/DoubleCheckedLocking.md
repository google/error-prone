Using double-checked locking on mutable objects in non-volatile fields is not
thread-safe.

If the field is not volatile, the compiler may re-order the code in the
accessor. For more information, see:

*   http://jeremymanson.blogspot.com/2008/05/double-checked-locking.html
*   http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
*   Java Concurrency in Practice, §16.2.4
*   Effective Java, Item 71

The canonical example of *correct* double-checked locking for lazy
initialization is:

```java
class Foo {
  /** This foo's bar.  Lazily initialized via double-checked locking. */
  private volatile Bar bar;

  public Bar getBar() {
    Bar value = bar;
    if (value == null) {
      synchronized (this) {
        value = bar;
        if (value == null) {
          bar = value = computeBar();
        }
      }
    }
    return value;
  }

  private Bar computeBar() { ... }
}
```

<!--
  TODO: Consider instead:
  - moving the synchronized block into a separate method to encourage getBar inlining
  - using (sharper) jdk9+ VarHandle.getAcquire together with VarHandle.setRelease
  - Suppliers.memoize
  - AtomicReference.updateAndGet()
-->

## Alternatives

Double-checked locking should only be used in performance critical classes. For
code that is less performance sensitive, there are simpler, more readable
approaches. Effective Java recommends two alternatives:

### Synchronized Accessors

For lazily initializing instance fields, consider a *synchronized accessor*. In
modern JVMs with efficient uncontended synchronization the performance
difference is often negligible.

```java
// Lazy initialization of instance field - synchronized accessor
private Object field;
synchronized Object get() {
  if (field == null) {
    field = computeValue();
  }
  return field;
}
```

### Holder Classes

If the field being initialized is static, consider using the *lazy
initialization holder class* idiom:

```java
// Lazy initialization holder class idiom for static fields
private static class Holder {
  static final Object field = computeValue();
}
static Object get() {
  return Holder.field;
}
```

## Double-checked locking and immutability

If the object being initialized with double-checked locking is
[immutable](http://jeremymanson.blogspot.com/2008/04/immutability-in-java.html),
then it is safe for the field to be non-volatile. *However*, the use of
volatile is still encouraged because it is almost free on x86 and makes the
code more obviously correct.

Note that immutable has a very specific meaning in this context:

> [An immutable object] is transitively reachable from a final field, has not
> changed since the final field was set, and a reference to the object
> containing the final field did not escape the constructor.

Double-checked locking on non-volatile fields is in general unsafe because the
compiler and JVM can re-order code from the object's constructor to occur
*after* the object is written to the field.

The final modifier prevents that re-ordering from occurring, and guarantees that
all of the object's final fields have been written to before a reference to that
object is published.
