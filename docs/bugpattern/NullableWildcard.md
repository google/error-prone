Nullness annotations directly on wildcard types are interpreted differently by
different tools.
— unless you are using the Checker Framework).

### Tool interpretations and suggested alternatives

#### `Foo<@Nullable ?>`

To the Checker Framework, this means that the type argument *must* be nullable.
They use this
[in `ExecutorService`](https://github.com/typetools/jdk/blob/1973fa0811588dd0bb025fdc99345cdb887b3b52/src/java.base/share/classes/java/util/concurrent/ExecutorService.java#L269).

To Kotlin, this means that the type argument *can* be nullable but need not be
so.

While Checker Framework users do sometimes want `Foo<@Nullable ?>`, we commonly
see them use it in places where `Foo<?>` would also be correct and would be more
flexible. To fully preserve Kotlin behavior, Kotlin users may wish to write
"`Foo<? extends @Nullable Object>`" (unless they are within the scope of
`@NullMarked`, in which case `Foo<?>` is equivalent).

The effects of that change would be:

*   It is a behavior change for the Checker Framework, one that could produce
    local or non-local the Checker Framework failures. But it's likely to be a
    desirable change except in the context of `ExecutorService` and the `Future`
    objects that it produces.
*   `Foo<?>` is probably not a behavior change for Kotlin except *outside* of
    `@NullMarked`, in which case the way to preserve behavior is to change to
    `Foo<? extends @Nullable Object>`.

#### `Foo<@NonNull ?>`

Both tools use this to mean that the type argument must be non-nullable. (See
[the Checker Framework docs](https://checkerframework.org/manual/#annotations-on-wildcards).)

We still recommend a no-op change to `Foo<? extends @NonNull Object>` (or,
within the scope of `@NullMarked`, `Foo<? extends Object>`) for consistency
reasons: In all other cases, an annotation directly on a wildcard or
[type parameter](NullableTypeParameter.md) can lead to different behavior from
different tools. (And even today, some lesser used tools treat `Foo<@NonNull ?>`
differently.)

### JSpecify specification

The JSpecify spec says that usages of their annotations on wildcard types are
unrecognized
([Javadoc](https://jspecify.dev/docs/api/org/jspecify/annotations/Nullable.html#applicability),
[spec](https://jspecify.dev/docs/spec/#recognized-locations-for-type-use-annotations)).
This specification choice is motivated by the disagreement in tool behavior
discussed above.
