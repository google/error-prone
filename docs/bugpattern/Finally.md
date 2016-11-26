Terminating a finally block abruptly preempts the outcome of the try and
catch blocks, and will cause the result of any previously executed return or
throw statements to be ignored. Finally blocks should be written so they always
complete normally.

Consider the following code. In the case where `doWork` throws
`SomeException`, the finally block will still be executed. If closing the
input stream *also* fails, then the exception that was thrown in the catch
block will be prempted by the exception thrown by `close()`, and the first
exception will be lost.

```java
InputStream in = openInputStream();
try {
  doWork(in);
} catch (SomeException e) {
  throw new SomeError(e);
} finally {
  in.close(); // exception could be thrown here
}
```

This code is easily fixed using try-with-resources. Below, the input stream will
always be closed, and if `doWork` fails and an `IOException` is thrown, then the
try-with-resources uses the `Throwable.addSuppressed()` method added in Java 7
to propagate both exceptions back to the caller.

```java
try (InputStream in = openInputStream()) {
  doWork(in);
} catch (SomeException e) {
  throw new SomeError(e);
}
```

If Java 7 is not available, we recommend Guava's
[Closer API](http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/io/Closer.html).

```java
Closer closer = Closer.create();
try {
  InputStream in = closer.register(openInputStream());
  doWork(in);
} catch (Throwable e) {
  throw closer.rethrow(e);
} finally {
  closer.close();
}
```

