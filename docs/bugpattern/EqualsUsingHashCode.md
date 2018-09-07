Don't implement `#equals` using just a `hashCode` comparison:

```java {.bad}
class MyClass {
  private final int a;
  private final int b;
  private final String c;

  ...

  @Override
  public boolean equals(@Nullable Object o) {
    return o.hashCode() == hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(a, b, c);
  }
```

The number of `Object`s with randomly distributed `hashCode` required to give a
50% chance of collision (and therefore, with this pattern, erroneously correct
equality) is only ~77k.
