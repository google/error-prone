This code uses `Object.equals` (or similar method) with a type that does not
have well-defined `equals` behavior: [`Collection`], [`Iterable`], [`Multimap`],
[`Queue`], or [`CharSequence`]. Such a call to `equals` may return `false` in
cases where equality was expected.

## For [`Collection`] or [`Iterable`]

Your code might be working correctly, but only if there is some *subtype* of
`Iterable` which *does* have well-defined equals behavior, and you are certain
that both operands are definitely of that type at runtime. (The common examples
of such types are [`List`], [`Set`], and [`Multiset`], or any subtypes of
those.)

If this describes your situation, congratulations: you don't have a bug. To make
this warning go away, change the references in your code to be of that more
specific static type, not [`Collection`] or [`Iterable`]. This lets the bug
checker (and human readers) *know* that there is no risk of a false negative.

The minimal solution is to cast or copy "just in time" before calling `equals`,
but ideally you can make broader changes, to adopt the proper interface more
widely in your code.

On the other hand, if you might be mixing a `List` and a non-`List`, etc., you
are at risk. If you can't correct that situation, one alternative solution is to
use [`Iterables.elementsEqual`] \(which checks for *order-dependent* equality,
like `List.equals`\).

## For [`Multimap`]

The discussion above generally applies in this case as well; the well-behaved
subtypes to choose from are [`ListMultimap`] and [`SetMultimap`].

There is no equivalent to [`Iterables.elementsEqual`] in this case, however.

## For [`Queue`]

All known `Queue` implementations besides [`LinkedList`] use reference equality
instead of value-based equality. *Some* of the workarounds discussed above may
apply.

## For [`CharSequence`]

When comparing a `String` to a `CharSequence`, prefer `String#contentEquals`.
When comparing the content of two `CharSequence`s, you may want to compare the
string representation: `lhs.toString().contentEquals(rhs)`.

[`Collection`]: https://docs.oracle.com/javase/8/docs/api/java/util/Collection.html
[`Iterable`]: https://docs.oracle.com/javase/8/docs/api/java/lang/Iterable.html
[`Iterables.elementsEqual`]: https://google.github.io/guava/releases/snapshot/api/docs/com/google/common/collect/Iterables.html#elementsEqual-java.lang.Iterable-java.lang.Iterable-
[`LinkedList`]: http://docs.oracle.com/javase/8/docs/api/java/util/LinkedList.html
[`ListMultimap`]: https://google.github.io/guava/releases/snapshot/api/docs/com/google/common/collect/ListMultimap.html
[`Multimap`]: https://google.github.io/guava/releases/snapshot/api/docs/com/google/common/collect/Multimap.html
[`Multiset`]: https://google.github.io/guava/releases/snapshot/api/docs/com/google/common/collect/Multiset.html
[`SetMultimap`]: https://google.github.io/guava/releases/snapshot/api/docs/com/google/common/collect/SetMultimap.html
[`Queue`]: http://docs.oracle.com/javase/8/docs/api/java/util/Queue.html
[`CharSequence`]: http://docs.oracle.com/javase/8/docs/api/java/lang/CharSequence.html
