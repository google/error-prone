Implementations of `#equals` should return `false` for different types, not
throw.

```java {.bad}
class Data {
  private int a;

  @Override
  public boolean equals(Object other) {
    Data that = (Data) other; // BAD: This may throw ClassCastException.
    return a == that.a;
  }
}
```
