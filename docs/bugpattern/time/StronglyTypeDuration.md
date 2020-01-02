Where possible, `java.time.Duration` fields should be strongly typed rather than
stored as integral primitives and converted at the usage site.

For example, rather than:

```java
public class X {
  private static final long TIMEOUT = 100;

  // later in the file
  use(Duration.ofMillis(TIMEOUT));
}
```

Prefer:

```java
public class X {
  private static final Duration TIMEOUT = Duration.ofMillis(100);

  // later in the file
  use(TIMEOUT);
}
```
