This `{@code }` tag isn't being recognised by the Javadoc parser. This can
happen very easily if the tag is malformed in some way.

Common cases include:

*   The tag not being terminated correctly, i.e. via a parenthesis (`{@code
    foo)`)
*   `{@code` being used where it isn't accepted, e.g. as the first argument to
    `@param` (`@param {@code myParam} a parameter`)
*   Unmatched curly braces in large blocks...

Unmatched curly braces can be particularly hard to spot, e.g.:

```java
/**
 * Should be used like:
 *
 * <pre>{@code
 *   Frobnicator frobnicator = new Frobnicator() {
 * }</pre>
 */
interface Frobnicator {}
```

Which should be:

```java
/**
 * Should be used like:
 *
 * <pre>{@code
 *   Frobnicator frobnicator = new Frobnicator() {
 *   };
 * }</pre>
 */
interface Frobnicator {}
```

## Suppression

Suppress by applying `@SuppressWarnings("UnrecognisedCodeBlock")` to the element
being documented.
