HTML entities used within `@code` and `@literal` tags will be interpreted
directly rather than converted to the expected characters. For example, this is
wrong:

```java {.bad}
/**
 * <pre>{@code
 *   &#064;Override
 *   public boolean equals(Object o) {
 *     return false;
 *   }
 * }</pre>
 */
```

An option is to drop the {@code } tags, though this will then require escaping
any generic type parameters which may otherwise be interpreted as HTML. That is,
`List<Integer>` is the text "List" followed by the (non-existent) tag "Integer".

```java {.good}
/**
 * <pre>
 *   &#064;Override
 *   public boolean equals(Object o) {
 *     return false;
 *   }
 * </pre>
 */
```

## Suppression

Suppress by applying `@SuppressWarnings("EscapedEntity")` to the element being
documented.
