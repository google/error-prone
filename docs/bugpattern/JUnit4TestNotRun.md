Unlike in JUnit 3, JUnit 4 tests will not be run unless annotated with @Test.
The test method that triggered this error looks like it was meant to be a test,
but was not so annotated, so it will not be run. If you intend for this test
method not to run, please add both an @Test and an @Ignore annotation to make it
clear that you are purposely disabling it. If this is a helper method and not a
test, consider reducing its visibility to non-public, if possible.
