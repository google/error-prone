If a test subject and the argument to `isEqualTo` are the same instance (e.g.
`assertThat(x).isEqualTo(x)`), then the assertion will always pass. Truth
implements `isEqualTo` using [`Objects#equal`] , which tests its arguments for
reference equality and returns true without calling `equals()` if both arguments
are the same instance.

[`Objects#equals`]: https://google.github.io/guava/releases/21.0/api/docs/com/google/common/base/Objects.html#equal-java.lang.Object-java.lang.Object-

To test the implementation of an `equals` method, use [Guava's
EqualsTester][javadoc].

[javadoc]: http://static.javadoc.io/com.google.guava/guava-testlib/21.0/com/google/common/testing/EqualsTester.html
