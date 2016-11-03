The [Google Java Style Guide §5.2.4][style] requires constant names to use
`CONSTANT_CASE`.

[style]: https://google.github.io/styleguide/javaguide.html#s5.2.4-constant-names

When naming a field with `CONSTANT_CASE`, make sure the field is `static`,
`final`, and of immutable type. If the field doesn't meet those criteria, use
`lowerCamelCase` instead.

The check recognizes all primitive, `String`, and `enum` fields as deeply
immutable. It is possible to create mutable enums, but doing so is
strongly discouraged.
