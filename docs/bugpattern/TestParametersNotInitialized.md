`@TestParameter` fields are only injected when the test class is run with
`@RunWith(TestParameterInjector.class)`. If the test class uses another runner
such as `@RunWith(JUnit4.class)`, JUnit does not initialize `@TestParameter`
fields, leaving them at their default Java values (`null`, `0`, or `false`).

```java
@RunWith(JUnit4.class)
public final class MyTest {
  @TestParameter boolean enabled;

  @Test
  public void myTest() {
    // `enabled` is always `false`!
  }
}
```

```java
@RunWith(TestParameterInjector.class)
public final class MyTest {
  @TestParameter boolean enabled;

  @Test
  public void myTest() {
    // Runs with `enabled = true` and `enabled = false`.
  }
}
```
