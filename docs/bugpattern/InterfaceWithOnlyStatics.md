Interfaces should be used to define types. Using an interface as a collection of
static methods and fields violates that, and can lead to confusing type
hierarchies if the interface is then implemented to allow easy access to the
constants.

Prefer using a `public final` class instead to prohibit subclassing.

```java {.bad}
public interface Constants {
  final float PI = 3.14159f;
}
```

```java {.good}
public final class Constants {
  public static final float PI = 3.14159f;

  private Constants() {}
}
```

