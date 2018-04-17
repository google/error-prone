If the body of the lambda passed to `assertThrows` contains multiple statements,
executation of the lambda will stop at the first statement that throws an
exception and all subsequent statements will be ignored.

Don't do this:

```java {.bad}
ImmutableList<Integer> xs = ImmutableList.of();
assertThrows(
    UnsupportedOperationException.class,
    () -> {
        xs.add(0);
        assertThat(xs).isEmpty(); // never executed!
    });
```

Do this instead:

```java {.good}
ImmutableList<Integer> xs = ImmutableList.of();
assertThrows(
    UnsupportedOperationException.class,
    () -> xs.add(0));
assertThat(xs).isEmpty();
```
