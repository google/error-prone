---
title: MutableMethodReturnType
summary: Method return type should use the immutable type (such as ImmutableList)
  instead of the general collection interface type (such as List).
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
For method return type, you should use the immutable type (such as
`ImmutableList`) instead of the general collection interface type (such as
`List`). This communicates to your callers important
[semantic guarantees][javadoc].

This is consistent with [Effective Java 3rd Edition §64][ej3e-64], which says to
refer to objects by their interfaces. Guava's immutable collection classes offer
meaningful behavioral guarantees -- they are not merely a specific
implementation as in the case of, say, `ArrayList`. They should be treated as
interfaces in every important sense of the word.

That is, prefer this:

```java
ImmutableList<String> getCountries() {
  return ImmutableList.of("Denmark", "Norway", "Sweden");
}
```

to this:

```java
List<String> getCountries() {
  return ImmutableList.of("Denmark", "Norway", "Sweden");
}
```

TIP: Using the immutable type for the method return type allows Error Prone to
prevent accidental attempts to modify the collection at compile-time (see
[`DoNotCall`]).

[`DoNotCall`]: https:errorprone.info/bugpattern/DoNotCall

[ej3e-64]: https://books.google.com/books?id=BIpDDwAAQBAJ
[javadoc]: https://guava.dev/releases/21.0/api/docs/com/google/common/collect/ImmutableCollection.html

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MutableMethodReturnType")` to the enclosing element.
