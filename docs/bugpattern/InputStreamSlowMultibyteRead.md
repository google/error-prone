`java.io.InputStream` defines a single abstract method: `int read()`, which
subclasses implement to return bytes from the logical input stream.

However, in most circumstances, readers from `InputStreams` use higher-level
methods like `read(byte[], int offset, int length)` to read multiple bytes at a
time into a buffer. The default implementation of this method is to repeatedly
call `read()`. However, most InputStream implementations could do much better if
they can read multiple bytes at once (at the very least, avoiding unneeded
`byte` -> `int` -> `byte` casts that are needed when implementing the read()
method over an underlying `byte` source).

The class in question implements `int read()` without also overriding `int
read(byte[], int, int)` and will thus be subject to the costs associated with
the default behavior of the multibyte read method.
