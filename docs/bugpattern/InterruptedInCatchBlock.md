When attempting to fail-fast when interrupted, you should preserve the
interrupted status of the thread by calling
`Thread.currentThread().interrupt()`:

```java
try {
  mightTimeOutOrBeCancelled(); // for example myFuture.get()
} catch (InterruptedException e) {
  Thread.currentThread().interrupt(); // Restore the interrupted status
  throw new MyCheckedException("[describe what task was interrupted]", e);
}
```

The current code is likely accidentally calling `Thread.interrupted()`, which
clears the interrupt bit. (That is typically a no-op in this situation because
the interrupt bit is generally already unset when `InterruptedException` has
been thrown.) `thread.interrupt()` correctly restores the interrupt bit.

More information:

*   https://web.archive.org/web/20201217182342/https://www.ibm.com/developerworks/java/library/j-jtp05236/index.html

*   [`thread.interrupt()`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Thread.html#interrupt\(\))

*   [`Thread.interrupted()`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Thread.html#interrupted\(\))
