BigDecimal's `double` can lose precision in surprising ways.

```java {bad}
  // these are the same:
  new BigDecimal(0.1)
  new BigDecimal("0.1000000000000000055511151231257827021181583404541015625")
```

Prefer the `BigDecimal.valueOf(double)` method or the `new BigDecimal(String)`
constructor.

NOTE `BigDecimal.valueOf(double)` does not suffer from the same problem; it is
equivalent to `new BigDecimal(Double.valueOf(double))`, and while `0.1` is not
exactly representable, `Double.valueOf(0.1)` yields `"0.1"`. As long as
[FloatingPointLiteralPrecision](./FloatingPointLiteralPrecision) doesn't
generate a warning, `BigDecimal.valueOf` is safe.
