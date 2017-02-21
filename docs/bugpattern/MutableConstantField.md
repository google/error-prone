The [Google Java Style Guide §5.2.4][style] requires every constant to be named
`CONSTANT_CASE` and defines it by an object which is deeply immutable and whose
methods have no detectable side effects. If you've initialized a constant with
an immutable type, e.g. `ImmutableList`, then its type should be `ImmutableList`
as well and not `List`, as immutability is not an implementation detail (like
`ArrayList`) but semantics.

[style]: https://google.github.io/styleguide/javaguide.html#s5.2.4-constant-names
