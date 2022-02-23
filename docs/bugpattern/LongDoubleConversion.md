A cast from `long` to `double` may lose precision. Prefer an explicit cast to an
implicit conversion if this was intentional.

Consider
[`com.google.protobuf.util.Values`](https://developers.google.com/protocol-buffers/docs/reference/java/com/google/protobuf/util/Values)
which has a method `of(double value)`, and the following example:

```java
// Values.of receives a long, which is implicitly converted to double:
long value = 123L;
Values.of(value);
```

Prefer this (to make existing behavior explicit):

```java
long value = 123L;
Values.of((double) value);
```

From [JLS §5.1.2]:

> A widening primitive conversion from `int` to `float`, or from `long` to
> `float`, or from `long` to `double`, may result in loss of precision - that
> is, the result may lose some of the least significant bits of the value. In
> this case, the resulting floating-point value will be a correctly rounded
> version of the integer value, using IEEE 754 round-to-nearest mode

[JLS §5.1.2]: https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html#jls-5.1.2
