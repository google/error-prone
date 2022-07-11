A non-standard Javadoc inline tag was used, or was used in the wrong way. For
example, `@param` should be used as a block tag to describe parameters, but
cannot be used inline to link to parameters (prefer `{@code paramName}` for
that).

```java
/**
 * Doubles {@param n}.
 */
int twoTimes(int n) {
  return 2 * n;
}
```

```java
/**
 * Doubles {@code n}.
 */
int twoTimes(int n) {
  return 2 * n;
}
```

If the `@` symbol occurrs inside a code excerpt, the fix is to escape the code
excerpt using `<pre>{@code ... }</pre>`:

```java
/**
 * Summary fragment.
 *
 * <pre>{@code
 * Your code here.
 * Can include <angle brackets>.
 * You can even include snippets that contain annotations, e.g.:
 * @Override public String toString() { ... }
 * }</pre>
 *
 * <p>Following paragraph.
 */
```

## Suppression

Suppress by applying `@SuppressWarnings("InvalidInlineTag")` to the element
being documented.
