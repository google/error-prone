Checks if `@NullMarked` annotation is applied to a Java class.

When you apply `@NullMarked` to a module, package, class, or method, it means
that unannotated types in that scope are treated as if they were annotated with
`@NonNull`, with some exceptions for local variables and type variables. In code
covered by `@NullMarked`, `String x` means the same as `@NonNull String x`.

This standardizes the default annotation for all the static checkers and
compilers that supports jspecify annotations.

For more information, see https://jspecify.dev/docs/user-guide/#nullmarked.

### Choice of Granularity

Typically, projects should choose only one of the following approaches and not
both:

*   **Package-level (`AddNullMarkedToPackageInfo`)**: Provides a concise way to
    null-mark an entire package at once. However, there are some drawbacks to
    relying on `package-info.java` in certain build environments, and it may
    lack the "locality of reference" provided by class-level annotations.
*   **Class-level (`AddNullMarkedToClass`)**: More robust and explicit, as the
    annotation is visible directly on each class declaration. This requires more
    annotations but can be clearer for readers.

For a project, it is recommended to either nullmark all the classes or all of
the `package-info` files, but not both.
