Object arrays are inferior to collections in almost every way. Prefer `Set`,
`List`, or `Multiset` over an object array whenever possible.

A few of these issues are covered, in much greater detail, in
[Effective Java Item 28: Prefer lists to arrays][ej3e-28].

# Method Parameters

Don't use object arrays as method parameters:

```java
public void createUsers(User[] users) { ... }
```

Use an `Iterable` instead:

```java
public void createUsers(Iterable<User> users) { ... }
```

# Return Values

Don't use object arrays as method return values:

```java
public User[] loadUsers() { ... }
```

Use an `ImmutableList` (or `ImmutableSet`) instead:

```java
public ImmutableList<User> loadUsers() { ... }
```

# Additional Alternatives

If you have a 2-dimensional array (e.g., `Foo[][]`), consider using an
`ImmutableTable<Integer, Integer, Foo>` instead.

[ej3e-28]: https://books.google.com/books?id=BIpDDwAAQBAJ
