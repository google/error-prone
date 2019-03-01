If the body of the lambda passed to `assertThrows` contains multiple statements,
execution of the lambda will stop at the first statement that throws an
exception and all subsequent statements will be ignored.

This means that:

*   Any set-up logic in the lambda will cause the test to incorrectly pass if it
    throws the expected exception.
*   Any assertions that run after the statement that throws will never be
    executed.

Don't do this:

```java {.bad}
assertThrows(
    UnsupportedOperationException.class,
    () -> {
        AppendOnlyList list = new AppendOnlyList();
        list.add(0, "a");
        list.remove(0);
        assertThat(list).containsExactly("a");
    });
```

Do this instead:

```java {.good}
AppendOnlyList list = new AppendOnlyList();
list.add(0, "a");
assertThrows(
    UnsupportedOperationException.class,
    () -> list.remove(0));
assertThat(list).containsExactly("a");
```
