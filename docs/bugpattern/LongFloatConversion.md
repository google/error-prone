A cast from `long` to `float` may lose precision. Prefer an explicit cast to an
implicit conversion if this was intentional.

Consider
[`java.awt.Color`](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Color.html)
which has constructors `Color(float r, float g, float b)` and `Color(int r, int
g, int b)`, and the following example:

```java
// Math.round returns a double, which is implicitly converted to float:
new Color(Math.round(18.0), Math.round(0.0), Math.round(18.0));
```

Prefer this (to make existing behavior explicit):

```java
new Color((float) Math.round(18.0), (float) Math.round(0.0), (float) Math.round(18.0));
```

or this (if this implicit conversion to `float` was unintentional):

```java
new Color((int) Math.round(18.0), (int) Math.round(0.0), (int) Math.round(18.0));
```

From [JLS §5.1.2]:

> A widening primitive conversion from `int` to `float`, or from `long` to
> `float`, or from `long` to `double`, may result in loss of precision - that
> is, the result may lose some of the least significant bits of the value. In
> this case, the resulting floating-point value will be a correctly rounded
> version of the integer value, using IEEE 754 round-to-nearest mode

[JLS §5.1.2]: https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html#jls-5.1.2
