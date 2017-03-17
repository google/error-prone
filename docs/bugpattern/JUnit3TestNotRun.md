JUnit 3 requires that test method names start with "`test`". The method that
triggered this error looks like it is supposed to be a test, but misspells the
required prefix; has `@Test` annotation, but no prefix; or has the wrong method
signature. As a consequence, JUnit 3 will ignore it.

If you meant to disable this test on purpose, or this is a helper method, change
the name to something more descriptive, like "`disabledTestSomething()`". You
don't need an `@Test` annotation, but if you want to keep it, add `@Ignore` too.
