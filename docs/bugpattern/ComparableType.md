The type argument of `Comparable` should always be the type of the current
class.

For example, do this:

```java {.good}
class Foo implements Comparable<Foo> {
  public int compareTo(Foo other) { ... }
}
```

not this:

```java {.bad}
class Foo implements Comparable<Bar> {
  public int compareTo(Foo other) { ... }
}
```

Implementing `Comparable` for a different type breaks the API contract, which
requires `x.compareTo(y) == -y.compareTo(x)` for all `x` and `y`. If `x` and `y`
are different types, this behaviour can't be guaranteed.
