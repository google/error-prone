Wherever possible, calls to Lock#lock should be immediately followed by a `try`
block with a `finally` clause which releases the lock,

```java {.good}
lock.lock();
try {
  frobnicate();
} finally {
  lock.unlock();
}
```

Placing the call to `lock` *inside* the `try` block is suboptimal. The
documentation for `Lock` allows for `Lock#lock` to throw an unchecked exception
if, for example, it determines that the program will deadlock. In this
situation, `#unlock` will be called even if `#lock` throws,

```java {.bad}
try {
  lock.lock();
  frobnicate();
} finally {
  lock.unlock();
}
```

Doing work between the `lock` invocation and the start of the `try` block is
potentially very bad, as the lock will go unreleased if the intermediate work
throws,

```java {.bad}
lock.lock();
checkState(frobnicator.ready());
try {
  frobnicator.frobnicate();
} finally {
  lock.unlock();
}
```

`frobnicator.ready()` should either be moved before the `#lock` call or inside
the `try` block, depending on whether the lock must be acquired before calling
it.
