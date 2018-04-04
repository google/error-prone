Calling a method that modifies a collection on an immutable implementation (e.g.
`ImmutableList.add`) is guaranteed to always throw an
`UnsupportedOperationException` and leave the collection unmodified.
