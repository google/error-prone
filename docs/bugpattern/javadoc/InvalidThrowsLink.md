`@throws` does not require `@link`ing the target exception.

```java
/**
 * Validates {@code n}.
 *
 * @throws {@link Exception} if n is negative.
 */
void validate(int n) throws Exception {
  ...
}
```

```java
/**
 * Validates {@code n}.
 *
 * @throws Exception if n is negative.
 */
void validate(int n) throws Exception {
  ...
}
```

## Suppression

Suppress by applying `@SuppressWarnings("InvalidThrowsLink")` to the element
being documented.
