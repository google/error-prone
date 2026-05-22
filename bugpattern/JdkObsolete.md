---
title: JdkObsolete
summary: Suggests alternatives to obsolete JDK classes.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Some JDK APIs are obsolete and have preferred alternatives.

## `LinkedList`

`LinkedList` almost never out-performs `ArrayList` or `ArrayDeque`[^1]. If you
are using `LinkedList` as a list, prefer `ArrayList`. If you are using
`LinkedList` as a stack or queue/deque, prefer `ArrayDeque`.

Migration gotcha: `LinkedList` permits `null` elements; `ArrayDeque` rejects
them. The documentation for `Deque` strongly discourages users from inserting
`null`, even into implementations that permit it. So, if you are using a
`LinkedList` for this purpose, you should likely stop, and you will _need_ to
stop in order to migrate to `ArrayDeque`.

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

When migrating from `Stack` to `Deque`, note that the `Stack` methods
`push`/`pop`/`peek`/`add`/`iterator` correspond to the `Deque` methods
`addFirst`/`removeFirst`/`peekFirst`/`addFirst`/`descendingIterator`.

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

## `Matcher#hitEnd()` and `Matcher#requireEnd()`

The methods [`Matcher#hitEnd()`][hitEnd] and
[`Matcher#requireEnd()`][requireEnd] are mainly intended for implementing
scanning functionalities (like `java.util.Scanner`), where you need to decide
whether to read more input from a stream to find a longer match.

If you are not implementing your own scanning or streaming parser, and the input
being matched against is already fully loaded in memory, using these methods is
likely a mistake.

### Check End of Match

If you want to check if a match extends to the end of the input, you can simply
compare the match end index with the length of the input string:

```java
// Instead of:
if (matcher.find() && matcher.hitEnd()) { ... }

// Use:
if (matcher.find() && matcher.end() == input.length()) { ... }
```

### Use java.util.Scanner

If you are implementing a scanner or streaming parser, consider using
`java.util.Scanner` directly, which already encapsulates this logic correctly.

```java
Scanner scanner = new Scanner(input);
while (scanner.hasNext(pattern)) {
  String match = scanner.next(pattern);
  ...
}
```

[^1]: People generally choose `LinkedList` because they want fast insertion and
    removal. However, `LinkedList` has slow traversal, and typically you need
    to traverse the list to find the place to insert/remove. It turns out that
    the cost of traversing to a location in a `LinkedList` is approximately 4x
    the cost of copying an element in an `ArrayList`. Thus, `LinkedList`'s
    traversal cost dominates and results in poorer performance. More info:
    https://stuartmarks.wordpress.com/2015/12/18/some-java-list-benchmarks/
[hitEnd]: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/regex/Matcher.html#hitEnd()
[requireEnd]: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/regex/Matcher.html#requireEnd()

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JdkObsolete")` to the enclosing element.
