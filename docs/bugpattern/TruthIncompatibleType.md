This assertion using Truth is guaranteed to be true or false based on the types
being passed to it. For example,

```java
Optional<Integer> x = Optional.of(1);
assertThat(x).isNotEqualTo(Optional.of(2L));
```

```java
ImmutableList<Long> xs = ImmutableList.of(1L, 2L);
assertThat(xs).doesNotContain(1);
```

These will always be true, given `Integer` is not comparable to `Long`. This
isn't such a big issue for `isEqualTo` assertions, given the test will fail.
However, it can be insidious for `isNotEqualTo` or `doesNotContain`, given the
assertion will be vacuously true without providing any test coverage.

## Testing equals methods

One false positive for this is where the goal is to test the `equals`
implementation of the type under test, i.e. that comparison to a different type
is `false` and does not throw:

```java
assertThat(myCustomType).isNotEqualTo("");
```

For such cases, consider whether your type can be implemented using `AutoValue`
to remove the need to implement `equals` by hand. If it can't, consider
[`EqualsTester`][javadoc].

```java
new EqualsTester()
    .addEqualityGroup(MyCustomType.create(1), MyCustomType.create(1))
    .addEqualityGroup("")
    .testEquals();
```

Although consider omitting an explicit comparison with a different type, as
`EqualsTester` does this already by default.

[javadoc]: http://static.javadoc.io/com.google.guava/guava-testlib/21.0/com/google/common/testing/EqualsTester.html
