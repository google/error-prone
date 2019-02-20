The `@throws` tag should not document a checked exception which is not actually
thrown by the documented method.

```java {.bad}
/**
 * Validates {@code n}.
 *
 * @throws Exception if n is negative.
 */
void validate(int n) {
  ...
}
```

## Suppression

Suppress by applying `@SuppressWarnings("InvalidThrows")` to the element being
documented.
