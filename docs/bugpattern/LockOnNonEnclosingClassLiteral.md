Having a lock on a class, other than the enclosing class of the code block, can
unintentionally prevent the locked class from being used properly when other
classes effectively lock on the same resource. From a maintainability
perspective, it can be time-consuming to ensure the `synchronized` blocks are
working as expected. Hence, locking on a class other than the enclosing class of
the `synchronized` code block is discouraged by Error Prone. Locking on the
enclosing class or an instance is a preferred practice.

For example, a lock on `Other.class` rather than `Example.class` will trigger an
Error Prone warning:

```java
class Example {
  void method() {
    synchronized (Other.class) {
    }
  }
}
```

A lock on the instance or the enclosing class of the `synchronized` code block
will not trigger the warning:

```java
class Example {
  void method() {
    synchronized (Example.class) {
    }
    synchronized (this) {
    }
  }
}
```
