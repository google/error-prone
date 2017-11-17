Binder is Android's inter-process communication mechanism.
Each call to `Binder.clearCallingIdentity()` should be followed by
`Binder.restoreCallingIdentity()` in a finally block. Otherwise the wrong
Binder identity may be used by subsequent code.

For example:
```java
long token = Binder.clearCallingIdentity();
// Issue a Binder call (may throw an Exception).
someBinderInterface.makeCall();
Binder.restoreCallingIdentity(token);
```

The above code should be rewritten as:
```java
long token = Binder.clearCallingIdentity();
try {
  // Issue a Binder call (may throw an Exception).
  someBinderInterface.makeCall();
} finally {
  Binder.restoreCallingIdentity(token);
}
```
