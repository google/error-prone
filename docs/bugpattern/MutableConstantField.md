For constant field declarations, you should use the immutable type (such as
`ImmutableList`) instead of the general collection interface type (such as
`List`). This communicates to your callers important [semantic
guarantees][javadoc].

This is consistent with [Effective Java Item 52][ej52], which says to refer to
objects by their interfaces. Guava's immutable collection classes offer
meaningful behavioral guarantees -- they are not merely a specific
implementation as in the case of, say, `ArrayList`. They should be treated as
interfaces in every important sense of the word.

That is, prefer this:

```java
static final ImmutableList<String> COUNTRIES =
    ImmutableList.of("Denmark", "Norway", "Sweden");
```

to this:

```java
static final List<String> COUNTRIES =
    ImmutableList.of("Denmark", "Norway", "Sweden");
```

TIP: Using the immutable type for the field declaration allows Error Prone to
prevent accidental attempts to modify the collection at compile-time (see
[`ImmutableModification`]).

[`ImmutableModification`]: https:errorprone.info/bugpattern/ImmutableModification

[ej52]: https://books.google.com/books?id=ka2VUBqHiWkC

[javadoc]: https://google.github.io/guava/releases/21.0/api/docs/com/google/common/collect/ImmutableCollection.html
