The [double-brace initialization pattern][dbi] should be avoided—especially in
non-static contexts.

The double-brace pattern uses an instance-initializer in an anonymous inner
class to express the initialization of a class (often a collection) in a single
step.

Inner classes in a non-static context are terrific sources of memory leaks! If
you pass the collection somewhere that retains it, the entire instance you
created it from can no longer be garbage collected. Even if it is completely
unreachable. And if someone serializes the map? Yep, the entire creating
instance goes along for the ride (or if that fails, serializing the map fails,
which is also awfully strange). All this is completely nonobvious.

Luckily, there are more readable and more performant alternatives in the factory
methods and builders for `ImmutableList`, `ImmutableSet`, and `ImmutableMap`.

The `List.of`, `Set.of`, and `Map.of` static factories
[added in Java 9](https://openjdk.java.net/jeps/269) are also a good choice.

That is, prefer this:

```java
ImmutableList.of("Denmark", "Norway", "Sweden");
```

Not this:

```java
new ArrayList<>() {
  {
    add("Denmark");
    add("Norway");
    add("Sweden");
  }
};
```

TIP: Neither the guava immutable collections nor the static factory methods
added in a JDK 9 support `null` elements. The double-brace pattern is still best
avoided for collections that contain null. Consider using `Arrays.asList` to
initialize `List`s and `Set`s with `null` values, and refactoring `Map`
initializers into a helper method.

[dbi]: https://stackoverflow.com/questions/1958636/what-is-double-brace-initialization-in-java
