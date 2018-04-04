Java assert statements may or may not be evaluated depending on runtime flags to
the JVM invocation. When used in tests, this means that the test assertions may
not be checked, and a test may pass when it should actually fail. To avoid this,
use one of the assertion libraries that are always enabled, such as JUnit's
`org.junit.Assert` or Google's Truth library.
