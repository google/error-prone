An equals method compares non-corresponding fields from itself and the other
instance:

```java {.bad}
class Frobnicator {
  private int a;
  private int b;

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof Frobnicator)) {
      return false;
    }
    Frobnicator that = (Frobnicator) other;
    return a == that.a && b == that.a; // BUG: should be b == that.b
  }
}
```
