`ByteBuffer` provides a view of an underlying bytes storage. The two most common
implementations are the non-direct byte buffers, which are backed by a bytes
array, and direct byte buffers, which usually are off-heap directly mapped to
memory. Thus, not all `ByteBuffer` implementations are backed by a bytes array,
and so it is needed to call `.hasArray()` before calling `.array()`.

On the other hand, the beginning of the array returned by `.array()` does not
necessarily correspond to the beginning of the `ByteBuffer`, and in this way,
accessing the returned array without knowing the `.arrayOffset()` may cause
undesired results.

For this reason, the `array()` method should only be used when the programmer
knows that the underlying ByteBuffer has a backing array (for example:
constructing the ByteBuffer with `ByteBuffer.wrap()` or `ByteBuffer.allocate()`)
. In addition, the `.arrayOffset()` should also be used to know the starting
position of the `ByteBuffer` content in such array.

Do this:

``` {.good}
// This will use the current buffer position
public void foo(ByteBuffer buffer) throws Exception {
   byte[] bytes = new byte[buffer.remaining()];
   buffer.get(bytes);
   buffer.position(buffer.position() - bytes.length); // Restores the buffer position
   // ...
}
```

or this:

``` {.good}
// This is an example of a correct usage of .array()
public void foo(ByteBuffer buffer) throws Exception {
   if (buffer.hasArray()) {
     bar(buffer.array(), /* offset */ buffer.arrayOffset() + buffer.position(),
         /* length */ buffer.remaining());
   }
   // ...
}
```

or this:

``` {.good}
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
