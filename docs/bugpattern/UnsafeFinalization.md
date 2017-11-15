When invoking `static` `native` methods from objects that implement
[`finalize`](https://docs.oracle.com/javase/9/docs/api/java/lang/Object.html#finalize--),
the finalizer may run before the native method finishes execution.

Consider the following example. During the execution of `run` as
`playAwesomeGame` won't hold the `this` pointer live potentially the finalizer
will run and cause the native code to being using freed data.

One possible fix is to make the native method not static.

> TIP: avoid implementing `finalize` whenever possible. For more information,
> see:
>
> *   Effective Java Item 7: Avoid finalizers
> *   The deprecation notice for `finalize`:
>
>     > The finalization mechanism is inherently problematic. Finalization can
>     > lead to performance issues, deadlocks, and hangs. Errors in finalizers
>     > can lead to resource leaks; there is no way to cancel finalization if it
>     > is no longer necessary; and no ordering is specified among calls to
>     > finalize methods of different objects. Furthermore, there are no
>     > guarantees regarding the timing of finalization. The finalize method
>     > might be called on a finalizable object only after an indefinite delay,
>     > if at all. Classes whose instances hold non-heap resources should
>     > provide a method to enable explicit release of those resources, and they
>     > should also implement AutoCloseable if appropriate.

```java
class MyAwesomeGame {
  // Instance field that owns memory.
  private long nativeResourcePtr;

  // Will allocate some native heap that this object "owns"
  private static native long doNativeInit();

  // Will release the native heap.
  private static native void cleanUpNativeResources(long nativeResourcePtr);

  // The native code that plays the game.
  private static native void playAwesomeGame(long nativeResourcePtr);

  public MyAwesomeGame() {
    nativeResourcePtr = doNativeInit();
  }

  @Override
  protected void finalize() {
    cleanUpNativeResources(nativeResourcePtr);
    nativeResourcePtr = 0;
    super.finalize();
  }

  public void run() {
    playAwesomeGame(nativeResourcePtr);
  }
}
```
