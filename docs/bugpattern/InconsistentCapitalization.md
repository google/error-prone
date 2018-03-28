It is confusing to have two or more variables under the same scope that differ
only in capitalization. Make sure that both of these follow the casing guide
([Google Java Style Guide §5.3][styleCamelCase]) and to be consistent if more
than one option is possible.

This checker will only find parameters that differ in capitalization with fields
that can be accessed from the parameter's scope.

[styleCamelCase]: https://google.github.io/styleguide/javaguide.html#s5.3-camel-case
