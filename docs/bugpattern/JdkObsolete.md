Some JDK APIs are obsolete and have preferred alternatives.

## `LinkedList`

It is very rare for `LinkedList` to out-perform `ArrayList` or `ArrayDeque`.
Avoid it unless you're willing to invest a lot of time into benchmarking.

## `Vector`

`Vector` performs synchronization that is usually unnecessary; prefer
`ArrayList`.

If a synchronized collection is necessary, use `Collections.synchronizedList` or
a data structure from `java.util.concurrent`.

## `Hashtable` and `Dictionary`

This is a nonstandard class that predates the Java Collections Framework; prefer
`LinkedHashMap` or `HashMap`.

If synchronization is necessary, `java.util.concurrent.ConcurrentHashMap` is
usually a good choice.

## `java.util.Stack`

`Stack` is a nonstandard class that predates the Java Collections Framework;
prefer `ArrayDeque`.

If a synchronized collection is necessary, use a data structure from
`java.util.concurrent`.

## `StringBuffer`

`StringBuffer` performs synchronization that is rarely necessary and has
significant performance overhead. Prefer `StringBuilder`, which does not do
synchronization.

If synchronization is necessary, consider creating an explicit lock object and
using `synchronized` blocks.

## `Enumeration`

An ancient precursor to `Iterator`.

## `SortedSet` and `SortedMap`

Replaced by `NavigableSet` and `NavigableMap` in Java 6.
