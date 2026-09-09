Consider using [Google's immutable collections][javadoc] (e.g., `ImmutableList.of(...)`) instead of the JDK's immutable collection factories (e.g., `List.of(...)`) that were introduced in Java 9.

## Comparison

Like the JDK immutable collection factories, Google's immutable collections:

*   Reject null elements
*   Prevent all modifications at runtime
*   Offer `copyOf` methods that avoid provably-unnecessary copies
*   Are open-sourced as part of [Guava](http://guava.dev)

But they also:

*   Have deterministic (insertion-ordered) iteration, instead of randomized
    order, which can lead to flaky tests.
*   Are public *types* that guarantee immutability, which make for far nicer
    field types and return types than the general-purpose types.
    *   This enables them to prevent most modification at
        [*compile-time*][`DoNotCall`]. In spirit, it's nearly as though the
        modification methods are not even there.
    *   It's never *your* responsibility to remember when to make a "defensive
        copy"; the compiler just tells you when you have to.
*   Have a richer set of construction paths (including builders), and a few
    other features like `ImmutableSet.asList()`.
*   Include immutable forms of additional collection types like multimaps.
*   Don't fail on calls to `contains(null)` (same as virtually every other
    collection you've ever used).
*   Are available for (and optimized for) Android.

The advantages of the JDK immutable collection factories are fewer:

*   No `com.google.common.collect` dependency (smaller code footprint).
*   Shorter class names (by 9 characters).
*   Will become (or, at some point, became) more popular than Google's immutable
    collections in the world at large.

Both libraries offer `copyOf` methods that skip making an actual copy in certain
cases. However, neither type (JDK or Google) can recognize the *other* as being
safely immutable. Therefore, codebases should strive for homogeneous immutable
collection usage.

[`DoNotCall`]: https://errorprone.info/bugpattern/DoNotCall
[javadoc]: https://guava.dev/ImmutableCollection
