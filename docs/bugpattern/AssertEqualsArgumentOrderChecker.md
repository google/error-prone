JUnit's assertEquals (and similar) are defined to take the expected value first
and the actual value second. Getting these the wrong way round will cause a
confusing error message if the assertion fails.
