When invoking `static native` methods from objects that override
[`finalize`](https://docs.oracle.com/javase/9/docs/api/java/lang/Object.html#finalize--),
the finalizer may run before the native method finishes execution.

Consider the following example:

```java
public class GameRunner {
  // Pointer to some native resource, stored as a long
  private long nativeResourcePtr;

  // Allocates the native resource that this object "owns"
  private static native long doNativeInit();

  // Releases the native resource
  private static native void cleanUpNativeResources(long nativeResourcePtr);

  public GameRunner() {
    nativeResourcePtr = doNativeInit();
  }

  @Override
  protected void finalize() {
    cleanUpNativeResources(nativeResourcePtr);
    nativeResourcePtr = 0;
    super.finalize();
  }

  public void run() {
    GameLibrary.playGame(nativeResourcePointer); // Bug!
  }
}

public class GameLibrary {
  // Plays the game using the native resource
  public static native void playGame(long nativeResourcePtr);
}
```

During the execution of `GameRunner.run`, the call to `playGame` may not
hold the `this` reference live, and its finalizer may run, cleaning up the
native resources while the native code is still executing.

You can fix this by making the `static native` method not `static`, or by
changing the `static native` method so that it is passed the enclosing instance
and not the `nativeResourcePtr` value directly.

If you can use Java 9, the new method
[`Reference.reachabilityFence`](https://docs.oracle.com/javase/9/docs/api/java/lang/ref/Reference.html#reachabilityFence-java.lang.Object-)
will keep the reference passed in live. Be sure to call it in a finally block:

```java
 public void run() {
    try {
      playAwesomeGame(nativeResourcePtr);
    } finally {
      Reference.reachabilityFence(this);
    }
  }
}
```

Note: This check doesn't currently detect this problem when passing fields from
objects other than "this". That is equally unsafe. Consider passing the Java
object instead.

## References

*   [Boehm, "Destructors, finalizers, and synchronization." POPL
    2003.](http://www.hpl.hp.com/techreports/2002/HPL-2002-335.html) Section 3.4
    discusses this problem.
*   [Java Language Specification 12.6.2, "Interaction with the Memory
    Model."](https://docs.oracle.com/javase/specs/jls/se9/html/jls-12.html#jls-12.6.2)
