A field initialized to hold an [immutable collection][javadoc] should be
declared using the `Immutable*` type itself (such as `ImmutableList`), not the
general collection interface type (such as `List`), or `Iterable`. This
communicates several very useful semantic guarantees to consumers, as explained
in the [documentation][javadoc].

Although these classes are *technically* not interfaces (in order to prevent
unauthorized implementations), they *are* actually interfaces in the sense used
by [Effective Java Item 52][ej52] ("Refer to objects by their interfaces").

So, prefer this:

```java
static final ImmutableList<String> COUNTRIES =
    ImmutableList.of("Denmark", "Norway", "Sweden");
```

over this:

```java
static final List<String> COUNTRIES =
    ImmutableList.of("Denmark", "Norway", "Sweden");
```

or this:

```java
static final Iterable<String> COUNTRIES =
    ImmutableList.of("Denmark", "Norway", "Sweden");
```

TIP: Using the immutable type for the field declaration allows Error Prone to
prevent accidental attempts to modify the collection at compile-time (see
[`ImmutableModification`]).

[`ImmutableModification`]: https://errorprone.info/bugpattern/ImmutableModification

[ej52]: https://books.google.com/books?id=ka2VUBqHiWkC

[javadoc]: https://google.github.io/guava/releases/snapshot-jre/api/docs/com/google/common/collect/ImmutableCollection.html
