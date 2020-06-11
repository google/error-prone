
Methods that may return `null` should be annotated with `@Nullable`. For
example, do this:

```java
public class Foo {
  @Nullable private String message = null;
  @Nullable public String getMessage() {
    return message;
  }
}
```

Not this:

```java
public class Foo {
  private String message = null;
  public String getMessage() {
    return message;
  }
}
```
