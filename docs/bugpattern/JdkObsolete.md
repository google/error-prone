Some JDK APIs are obsolete and have preferred alternatives.

## `LinkedList`

`LinkedList` almost never out-performs `ArrayList` or `ArrayDeque`[^1].
If you are using `LinkedList` as a list, prefer `ArrayList`.  If you are using
`LinkedList` as a stack or queue/deque, prefer `ArrayDeque`.

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

[^1]: People generally choose `LinkedList` because they want fast insertion and
  removal.  However, `LinkedList` has slow traversal, and typically you need to
  traverse the list to find the place to insert/remove.  It turns out that
  the cost of traversing to a location in a `LinkedList` is approximately 4x
  the cost of copying an element in an `ArrayList`.  Thus, `LinkedList`'s
  traversal cost dominates and results in poorer performance.

  More info: https://stuartmarks.wordpress.com/2015/12/18/some-java-list-benchmarks/
