

Fields that may be `null` should be annotated with `@Nullable`. For example, do
this:

```java
public class Foo {
  @Nullable private String message = "hello";
  public void reset() {
    message = null;
  }
}
```

Not this:

```java
public class Foo {
  private String message = "hello";
  public void reset() {
    message = null;
  }
}
```
