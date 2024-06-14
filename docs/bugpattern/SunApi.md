Warn on internal, proprietary APIs that may be removed in future JDK versions.

For `sun.misc.Unsafe`, note that the API will be removed from a future version
of the JDK:
[JEP 471: Deprecate the Memory-Access Methods in sun.misc.Unsafe for Removal](https://openjdk.org/jeps/471).

This check is a re-implementation of javac's 'sunapi' diagnostic.
