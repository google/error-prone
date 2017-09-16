NullPointerTester comes with built-in support for some well known types like
`Optional` and `ImmutableList` via guava's
[`ArbitraryInstances`](http://static.javadoc.io/com.google.guava/guava-testlib/23.0/com/google/common/testing/ArbitraryInstances.html)
class. Explicitly calling `setDefault` for these types is unnecessary.
