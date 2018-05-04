`ByteBuffer` provides a view of an underlying bytes storage. The two most common
implementations are the non-direct byte buffers, which are backed by a bytes
array, and direct byte buffers, which usually are off-heap directly mapped to
memory. Thus, not all `ByteBuffer` implementations are backed by a bytes array,
and when they are, the beginning of the array returned by `.array()` may not
necessarily correspond to the beginning of the `ByteBuffer`.

Since it's so finicky, **use of `.array()` is discouraged. Use `.get(...)` to
copy the underlying data instead.**

But, if you *absolutely must* use `.array()` to look behind the `ByteBuffer`
curtain, check all of the following:

*   Ensure there is a backing array with `.hasArray()`
*   Check the start position of the backing array by calling `.arrayOffset()`
*   Check the end position of the backing array with `.remaining()`

If you know that the `ByteBuffer` was created locally with
`ByteBuffer.wrap(...)` or `ByteBuffer.allocate(...)`, it's safe, you can use the
backing array normally, without checking the above.

Do this:

``` {.good}
// Use `.get(...)` to copy the byte[] without changing the current position.
public void foo(ByteBuffer buffer) throws Exception {
   byte[] bytes = new byte[buffer.remaining()];
   buffer.get(bytes);
   buffer.position(buffer.position() - bytes.length); // Restores the buffer position
   // ...
}
```

or this:

``` {.good}
// Use `.array()` only if you also check `.hasArray()`, `.arrayOffset()`, and `.remaining()`.
public void foo(ByteBuffer buffer) throws Exception {
  if (buffer.hasArray()) {
    int startIndex = byteBuffer.arrayOffset();
    int curIndex = byteBuffer.arrayOffset() + byteBuffer.position();
    int endIndex = curIndex + byteBuffer.remaining();
    // Access elements of `.array()` with the above indices ...
  }
}
```

or this:

``` {.good}
// No checking necessary when the buffer was constructed locally with `allocate(...)`.
public void foo() throws Exception {
      ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
      buffer.putLong(1L);
      // ...
      buffer.array();
      // ...
}
```

or this:

``` {.good}
// No checking necessary when the buffer was constructed locally with `wrap(...)`.
public void foo(byte[] bytes) throws Exception {
   ByteBuffer buffer = ByteBuffer.wrap(bytes);
   // ...
   buffer.array();
   // ...
}
```

Not this:

``` {.bad}
public void foo(ByteBuffer buffer) {
   byte[] dataAsBytesArray = buffer.array();
   // ...
}
```
