Some JDK APIs are obsolete and have preferred alternatives.

## `LinkedList`

It is very rare for `LinkedList` to out-perform `ArrayList` or `ArrayDeque`.
Avoid it unless you're willing to invest a lot of time into benchmarking.

If a synchronized collection is necessary, use `Collections.synchronizedList` or
a data structure from `java.util.concurrent`.

## `Vector`

`Vector` performs synchronization that is usually unnecessary; prefer
`ArrayList`.

If a synchronized collection is necessary, use `Collections.synchronizedList` or
a data structure from `java.util.concurrent`.

## `Hashtable`

This is a nonstandard class that predate the Java Collections Framework; prefer
`LinkedHashMap`.

If synchronization is necessary, `java.util.concurrent.ConcurrentHashMap` is
usually a good choice.

## `java.util.Stack`

`Stack` is a nonstandard class that predates the Java Collections Framework;
prefer `ArrayDeque`.

## `StringBuffer`

`StringBuffer` performs synchronization that is rarely necessary and has
significant performance overhead. Prefer `StringBuilder`, which does not do
synchronization.

If synchronization is necessary, consider creating an explicit lock object and
using `synchronized` blocks.
