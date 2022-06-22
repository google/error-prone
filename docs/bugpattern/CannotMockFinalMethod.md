Mockito cannot mock `final` methods, and cannot tell at runtime that this is
attempted and fail with an error (as mocking `final` classes does).

`when(mock.finalMethod())` will invoke the real implementation of `finalMethod`.
In some cases, this may wind up accidentally doing what's intended:

```java
when(converter.convert(a)).thenReturn(b);
```

`convert` is final, but under the hood, calls `doForward`, so we wind up mocking
that method instead.
