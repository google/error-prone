Classes that AssistedInject factories create may not be annotated with scope
annotations, such as @Singleton. This will cause a Guice error at runtime.

See [https://code.google.com/p/google-guice/issues/detail?id=742 this bug
report] for details.
