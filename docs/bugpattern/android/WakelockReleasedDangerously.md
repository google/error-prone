If `release()` is called on an already-released `WakeLock`, a `RuntimeException`
is thrown, which can crash your app if not handled properly.

This may happen even in cases where you don't expect it. For example:

```java
wakelock.acquire(TIMEOUT_MS);
...
if (wakelock.isHeld()) {
  wakelock.release();
}
```

Looks fine, right? *Nope.* Since this wakelock was acquired with a timeout, the
timeout may expire and the wakelock may be released by the system, even between
the `isHeld()` check and `release()`. If the system releases the wakelock, and
the programmer calls `release()` again, a `RuntimeException` will be thrown, and
the app may crash.

Note: The suggested fix removes checks for `wakelock.isHeld()`, for this very
reason. This pattern - checking if a wakelock with timeout is held before
releasing - is subject to a race condition, so leaving this call in the code is
ineffectual and misleading.

To prevent crashes like this, `WakeLock`s acquired with timeout should be
released *only in a `try/catch(RuntimeException)` block*.

This does not hold for `WakeLock`s that are [not reference
counted](https://android.googlesource.com/platform/frameworks/base/+/nougat-release/core/java/android/os/PowerManager.java#1267).
`WakeLock`s are reference counted by default, but if
`setReferenceCounted(false)` has been called on the `WakeLock` in question, the
OS does not check whether the `WakeLock` has been released too many times, and
no `RuntimeException` is thrown.


This check will flag any call of any overload of `wakelock.release()` that is:

*   not in a try block that has a clause to catch `RuntimeException`
*   called on a WakeLock instance that is:
    *   acquired with a timeout in the same class
    *   and is reference counted (i.e., has not had the default changed via
        `wakelock.setReferenceCounted(false)`)
