The contract for `Comparator#compare` and `Comparable#compareTo` states that the
result is an integer which is `< 0` for less than, `== 0` for equality and `> 0`
for greater than. While most implementations return `-1`, `0` and `+1` for those
cases respectively, this is not guaranteed. Always comparing to `0` is the
safest use of the return value.

```java {.bad}
  boolean <T> isLessThan(Comparator<T> comparator, T a, T b) {
    // Fragile: it's not guaranteed that `comparator` returns -1 to mean
    // "less than".
    return comparator.compare(a, b) == -1;
  }
```

```java {.good}
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

```java {.good}
  boolean <T> greaterThan(Comparator<T> comparator, T a, T b) {
    return comparator.compare(a, b) > 0;
  }
```
