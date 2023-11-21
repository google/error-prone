This comment starts with `/**`, but isn't actually Javadoc.

Javadoc comments
[must precede a class, field, constructor, or method declaration](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html#format).

Using `/**` comments in locations where Javadoc is not supported is confusing
and unnecessary.

Suggested solutions:

*   If the comment is intended to be part of the API documentation, move it to
    the appropriate class, field, constructor, or method declaration.

*   If the comment is intended to be an implementation comment, use a
    single-line `//` or a multi-line `/*` comment instead.

## Suppression

Suppress by applying `@SuppressWarnings("NotJavadoc")` to the enclosing element.
