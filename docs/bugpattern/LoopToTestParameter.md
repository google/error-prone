Iterating over test inputs inside a `@Test` method executes all cases in a
single test invocation: the first failure aborts the remaining cases, and test
reports do not identify which input failed.

Using `TestParameterInjector` with `@TestParameter` runs each input case in
strict isolation with per-case reporting:

```java
@Test
public void parseValidUnits() {
  for (TimeUnit unit : TimeUnit.values()) {
    assertThat(parseUnit(unit.name())).isEqualTo(unit);
  }
}
```

```java
@RunWith(TestParameterInjector.class)
public final class MyTest {
  @Test
  public void parseValidUnits(@TestParameter TimeUnit unit) {
    assertThat(parseUnit(unit.name())).isEqualTo(unit);
  }
}
```

For explicit subsets of values, pass the elements to `@TestParameter`:

```java
@Test
public void acceptsKnownSchemes() {
  for (String scheme : ImmutableList.of("http", "https")) {
    assertThat(isValidScheme(scheme)).isTrue();
  }
}
```

```java
@RunWith(TestParameterInjector.class)
public final class MyTest {
  @Test
  public void acceptsKnownSchemes(@TestParameter({"http", "https"}) String scheme) {
    assertThat(isValidScheme(scheme)).isTrue();
  }
}
```
