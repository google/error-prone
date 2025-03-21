Pattern matching with `instanceof` allows writing this:

```java
void handle(Object o) {
  if (o instanceof Point(int x, int y)) {
    handlePoint(x, y);
  } else if (o instanceof String s) {
    handleString(s);
  }
}
```

which is more concise than an instanceof and a separate cast:

```java
void handle(Object o) {
  if (o instanceof Point) {
    Point point = (Point) o;
    handlePoint(point.x(), point.y());
  } else if (o instanceof String) {
    String s = (String) o;
    handleString(s);
  }
}
```

If the flag `-XepOpt:PatternMatchingInstanceof:EnableNegatedMatches=false` is used,
only casts that are in the same expression as the instanceof
or in the block that starts with the instanceof are rewritten,
but casts inside else branches or below an if statement with a negated instanceof
are left unchanged.

For more information on pattern matching and `instanceof`, see
[Pattern Matching for the instanceof Operator](https://docs.oracle.com/en/java/javase/21/language/pattern-matching-instanceof.html)
