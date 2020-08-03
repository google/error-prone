Nonzero-length arrays are mutable. Declaring one `public static final` indicates
that the developer expects it to be a constant, which is not the case. Making it
`public` is especially dangerous since clients of this code can modify the
contents of the array.

There are two ways to fix this problem:

1.  Refactor the array to an `ImmutableList`.
2.  Make the array `private` and add a `public` method that returns a copy of
    the `private` array.

See [Effective Java 3rd Edition §15][ej3e-15] for more details.

[ej3e-15]: https://books.google.com/books?id=BIpDDwAAQBAJ
