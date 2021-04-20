Classes that AssistedInject factories create may not be annotated with scope
annotations, such as @Singleton. This will cause a Guice error at runtime.

See
[this bug report](https://code.google.com/p/google-guice/issues/detail?id=742)
for details.
