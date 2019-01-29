Javadocs are interpreted as HTML, so special characters such as `<`, `>`, and
`&` must be escaped.

Text within `@code`, `@literal` and `@link` tags is exempt from this.

```java {.bad}
/** Returns whether n > 3. */
boolean greaterThanThree(int n);
```

Could be rendered as one of these instead:

```java {.good}
/** Returns whether n &gt; 3. */
boolean greaterThanThree(int n);

/** Returns whether {@code n > 3}. */
boolean greaterThanThree(int n);
```

A common pitfall is type parameters. The following Javadoc is valid, but
contains an unknown HTML tag (`Integer`):

```java {.bad}
/** Returns an Iterable<Integer> of prime numbers. */
Iterable<Integer> generatePrimes();
```

Prefer writing generic types as `{@code Iterable<Integer>}` (or `{@link }`).

## Suppression

Suppress by applying `@SuppressWarnings("UnescapedEntity")` to the element being
documented.
