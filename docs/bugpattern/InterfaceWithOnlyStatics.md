Interfaces should be used to define types. Using an interface as a collection of
static methods and fields violates that, and can lead to confusing type
hierarchies if the interface is then implemented to allow easy access to the
constants.

Prefer using a `public final` class instead to prohibit subclassing.

```java
public interface Constants {
  final float PI = 3.14159f;
}
```

```java
public final class Constants {
  public static final float PI = 3.14159f;

  private Constants() {}
}
```

See
[Effective Java 3rd Edition §22: Use interfaces only to define types][ej3e-22]
for more details.

[ej3e-22]: https://books.google.com/books?id=BIpDDwAAQBAJ
