The boolean operators `&&` and `||` should almost always be used instead of `&`
and `|`.

If the right hand side is an expression that has side effects or is expensive to
compute, `&&` and `||` will short-circuit but `&` and `|` will not, which may be
surprising or cause slowness.

If evaluating both operands is necessary for side effects, consider refactoring
to make that explicit. For example, prefer this:

```java
boolean rhs = hasSideEffects();
if (lhs && rhs) {
  // ...
}
```

to this:

```java
if (lhs & hasSideEffects()) {
  // ...
}
```
