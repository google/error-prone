NullPointerTester comes with built-in support for some well known types like
`Optional` and `ImmutableList` via guava's
[`ArbitraryInstances`](https://javadoc.io/doc/com.google.guava/guava-testlib/latest/com/google/common/testing/ArbitraryInstances.html)
class. Explicitly calling `setDefault` for these types is unnecessary.
