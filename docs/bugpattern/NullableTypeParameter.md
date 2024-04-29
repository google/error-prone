Nullness annotations directly on type parameters are interpreted differently by
different tools.
— unless you are using the Checker Framework.

### Tool interpretations and suggested alternatives

#### `class Foo<@Nullable T>`

To the Checker Framework, this means that a given type argument *must* be
nullable. For example, in the Checker Framework's default JDK stubs,
[the type argument to `ThreadLocal` must be nullable](https://github.com/typetools/jdk/blob/1973fa0811588dd0bb025fdc99345cdb887b3b52/src/java.base/share/classes/java/lang/ThreadLocal.java#L84-L91).

To Kotlin, this means that the type argument *can* be nullable but need not be
so.

If you want the Checker Framework interpretation, then keep your code as it is,
and suppress this warning. If you want the "*can* be nullable" interpretation,
change to `class Foo<T extends @Nullable Object>`.

The effects of that change would be:

-   It is a behavior change for the Checker Framework, one that could even
    produce local or non-local the Checker Framework failures. As discussed
    above, it may be a desirable change, and it's likely to be safe unless you
    are using it with `ThreadLocal`.

-   This is probably not a behavior change for Kotlin.

#### `class Foo<@NonNull T>`

To the Checker Framework, this means to allow *any* nullness for the type
argument(!). (It
[sets the *lower* bound](https://checkerframework.org/manual/#generics-bounds-syntax)
to `@NonNull`, which is already the default there.)

To Kotlin, this means that the type argument must be non-nullable.

Users probably want `class Foo<T extends @NonNull Object>`. Or, if they're
within the scope of `@NullMarked` and they are using tools that recognize it
(such as Kotlin), they may prefer `class Foo<T>`, which is equivalent but
shorter.

The effects of that change would be:

*   It is a behavior change for the Checker Framework, one that could produce
    local or non-local the Checker Framework failures. But it's likely to be a
    desirable change.
*   It is probably not a behavior change for Kotlin.

### JSpecify specification

The JSpecify spec says that usages of their annotations on type parameters are
unrecognized
([Javadoc](https://jspecify.dev/docs/api/org/jspecify/annotations/Nullable.html#applicability),
[spec](https://jspecify.dev/docs/spec/#recognized-locations-for-type-use-annotations)).
This specification choice is motivated by the disagreement in tool behavior
discussed above.
