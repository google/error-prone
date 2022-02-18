---
title: PreferredInterfaceType
summary: This type can be more specific.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: MutableConstantField, MutableMethodReturnType_

## The problem
Where possible, prefer referring to some types by a subtype which conveys more
information. For example: `ImmutableList` conveys more information (the
[immutability][javadoc] of the type). than `List`, `List` conveys more
information than `Collection`, and `ListMultimap` conveys more information than
`Multimap`.

This is consistent with [Effective Java 3rd Edition §64][ej3e-64], which says to
refer to objects by their interfaces.

*   Guava's immutable collection classes offer meaningful behavioral
    guarantees -- they are not merely a specific implementation as in the case
    of, say, `ArrayList`. They should be treated as interfaces in every
    important sense of the word.
*   Similarly, `Multimap` was designed as a superinterface for `ListMultimap`
    and `SetMultimap` which should [rarely be referred to directly][multimap].

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
[multimap]: https://github.com/google/guava/wiki/NewCollectionTypesExplained#multimap

[ej3e-64]: https://books.google.com/books?id=BIpDDwAAQBAJ
[javadoc]: https://guava.dev/releases/21.0/api/docs/com/google/common/collect/ImmutableCollection.html

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PreferredInterfaceType")` to the enclosing element.
