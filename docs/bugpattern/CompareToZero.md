The contract for `Comparator#compare` and `Comparable#compareTo` states that the
result is an integer which is `< 0` for less than, `== 0` for equality and `> 0`
for greater than. While most implementations return `-1`, `0` and `+1` for those
cases respectively, this is not guaranteed. Always comparing directly against
`0` is the safest use of the return value.

```java
  boolean <T> isLessThan(Comparator<T> comparator, T a, T b) {
    // Fragile: it's not guaranteed that `comparator` returns -1 to mean "less than".
    return comparator.compare(a, b) == -1;
  }
```

```java
  boolean <T> isLessThan(Comparator<T> comparator, T a, T b) {
    return comparator.compare(a, b) < 0;
  }
```

Even comparisons which are otherwise correct are clearer to other readers of the
code if turned into a comparison to `0`, e.g.:

```java
  boolean <T> greaterThan(Comparator<T> comparator, T a, T b) {
    return comparator.compare(a, b) >= 1;
  }
```

```java
  boolean <T> greaterThan(Comparator<T> comparator, T a, T b) {
    return comparator.compare(a, b) > 0;
  }
```

When comparing against `0`, `0` should always be on the right-hand side of the
operator so that `a.compareTo(b) <op> 0` mirrors the relationship `a <op> b`:

```java
  boolean <T> greaterThan(Comparable<T> a, T b) {
    // Confusing: `<` is used to check that `a` is greater than `b`.
    return 0 < a.compareTo(b);
  }
```

```java
  boolean <T> greaterThan(Comparable<T> a, T b) {
    return a.compareTo(b) > 0;
  }
```

Similarly, when switching on the result of `compare` or `compareTo` in a
`switch` statement or expression, the selector must be wrapped in
`Integer.signum()` to normalize the result to `-1`, `0`, or `1`:

```java
  switch (comparator.compare(a, b)) {
    case -1 -> ...
    case 0 -> ...
    default -> ...
  }
```

```java
  switch (signum(comparator.compare(a, b))) {
    case -1 -> ...
    case 0 -> ...
    default -> ...
  }
```
