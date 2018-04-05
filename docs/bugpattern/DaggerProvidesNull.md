Dagger `@Provides` methods may not return null unless annotated with
`@Nullable`. Such a method will cause a `NullPointerException` at runtime if the
`return null` path is ever taken.

If you believe the `return null` path can never be taken, please throw a
`RuntimeException` instead. Otherwise, please annotate the method with
`@Nullable`.
