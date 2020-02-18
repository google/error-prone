Passing a literal `null` to an `Optional` accepting parameter is likely a bug.
`Optional` is already designed to encode missing values through a non-`null`
instance.

```java
Optional<Integer> double(Optional<Integer> i) {
  return i.map(i -> i * 2);
}

Optional<Integer> doubled = double(null);
```

```java
Optional<Integer> doubled = double(Optional.empty());
```

This is a scenario that can easily happen when refactoring code from accepting
`@Nullable` parameters to accept `Optional`s. Note that the check will not match
if the parameter is explicitly annotated `@Nullable`.
