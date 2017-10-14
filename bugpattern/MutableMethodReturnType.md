---
title: MutableMethodReturnType
summary: Method return type should use the immutable type (such as ImmutableList) instead of the general collection interface type (such as List)
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
For method return type, you should use the immutable type (such as
`ImmutableList`) instead of the general collection interface type (such as
`List`). This communicates to your callers important [semantic
guarantees][javadoc].

This is consistent with [Effective Java Item 52][ej52], which says to refer to
objects by their interfaces. Guava's immutable collection classes offer
meaningful behavioral guarantees -- they are not merely a specific
implementation as in the case of, say, `ArrayList`. They should be treated as
interfaces in every important sense of the word.

That is, prefer this:

```java {.good}
ImmutableList<String> getCoutries() {
  return ImmutableList.of("Denmark", "Norway", "Sweden");
}
```

to this:

```java {.bad}
List<String> getCoutries() {
  return ImmutableList.of("Denmark", "Norway", "Sweden");
}
```

TIP: Using the immutable type for the method return type allows Error Prone to
prevent accidental attempts to modify the collection at compile-time (see
[`ImmutableModification`]).

[`ImmutableModification`]: https:errorprone.info/bugpattern/ImmutableModification

[ej52]: https://books.google.com/books?id=ka2VUBqHiWkC

[javadoc]: https://google.github.io/guava/releases/21.0/api/docs/com/google/common/collect/ImmutableCollection.html

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MutableMethodReturnType")` annotation to the enclosing element.
