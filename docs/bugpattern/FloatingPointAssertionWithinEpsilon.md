Both JUnit and Truth allow for asserting equality of floating point numbers with
an absolute tolerance. For example, the following statements are equivalent,

```java
double EPSILON = 1e-20;
assertThat(actualValue).isWithin(EPSILON).of(Math.PI);
assertEquals(Math.PI, actualValue, EPSILON);
```

What's not immediately obvious is that both of these assertions are checking
exact equality between `Math.PI` and `actualValue`, because the next `double`
after `Math.PI` is `Math.PI + 4.44e-16`.

This means that using the same tolerance to compare several floating point
values with different magnitude can be prone to error,

```java
float TOLERANCE = 1e-5f;
assertThat(pressure).isWithin(TOLERANCE).of(1f); // GOOD
assertThat(pressure).isWithin(TOLERANCE).of(10f); // GOOD
assertThat(pressure).isWithin(TOLERANCE).of(100f); // BAD -- misleading equals check
```

A larger tolerance should be used if the goal of the test is to allow for some
floating point errors, or, if not, `isEqualTo` makes the intention more clear.
