A non-standard Javadoc inline tag was used, or was used in the wrong way. For
example, `@param` should be used as a block tag to describe parameters, but
cannot be used inline to link to parameters (prefer `{@code paramName}` for
that).

```java {.bad}
/**
 * Doubles {@param n}.
 */
int twoTimes(int n) {
  return 2 * n;
}
```

```java {.good}
/**
 * Doubles {@code n}.
 */
int twoTimes(int n) {
  return 2 * n;
}
```

## Suppression

Suppress by applying `@SuppressWarnings("InvalidInlineTag")` to the element
being documented.
