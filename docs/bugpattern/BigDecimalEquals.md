`BigDecimal`'s equals method compares the scale of the representation as well as
the numeric value, which may not be expected.

```java
BigDecimal a = new BigDecimal("1.0");
BigDecimal b = new BigDecimal("1.00");
a.equals(b); // false!
```

