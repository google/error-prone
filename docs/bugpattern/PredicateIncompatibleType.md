Using `Foo::equals` as a `Predicate` for any type that is not compatible with
`Foo` is almost certainly a bug, since the predicate will always return false.

For example, consider:

```java
Predicate<Integer> p = "hello"::equals;
```

See also [EqualsIncompatibleType](EqualsIncompatibleType.md).
