Instances of boxed primitive types may be cached by the standard library
`valueOf` method. This method is used for autoboxing. This means that using a
boxed primitive as a lock can result in unintentionally sharing a lock with
another piece of code.

Consider using an explicit lock `Object` instead of locking on a boxed
primitive. That is, prefer this:

```java
private final Object lock = new Object();

void doSomething() {
  synchronized (lock) {
    // ...
  }
}
```

instead of this:

```java
private final Integer lock = 42;

void doSomething() {
  synchronized (lock) {
    // ...
  }
}
```
