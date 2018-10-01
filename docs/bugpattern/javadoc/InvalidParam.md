Javadoc's `@param` tag should not be used to document parameters which do not
appear in the formal parameter list. This may indicate a typo, or an omission
when refactoring.

```java {.bad}
/**
 * Parses {@code input} as a proto.
 *
 * @param inputBytes input bytes to parse.
 */
MyProto parse(byte[] input) {
  ...
}
```
