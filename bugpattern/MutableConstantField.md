---
title: MutableConstantField
summary: Constant field declarations should use the immutable type (such as ImmutableList) instead of the general collection interface type (such as List)
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
A field initialized to hold an [immutable collection][javadoc] should be
declared using the `Immutable*` type itself (such as `ImmutableList`), not the
general collection interface type (such as `List`), or `Iterable`. This
communicates several very useful semantic guarantees to consumers, as explained
in the [documentation][javadoc].

Although these classes are *technically* not interfaces (in order to prevent
unauthorized implementations), they *are* actually interfaces in the sense used
by [Effective Java Item 64][ej3e-64] ("Refer to objects by their interfaces").

So, prefer this:

```java
static final ImmutableList<String> COUNTRIES =
    ImmutableList.of("Denmark", "Norway", "Sweden");
```

over this:

```java
static final List<String> COUNTRIES =
    ImmutableList.of("Denmark", "Norway", "Sweden");
```

or this:

```java
static final Iterable<String> COUNTRIES =
    ImmutableList.of("Denmark", "Norway", "Sweden");
```

TIP: Using the immutable type for the field declaration allows Error Prone to
prevent accidental attempts to modify the collection at compile-time (see
[`ImmutableModification`]).

[`ImmutableModification`]: https://errorprone.info/bugpattern/ImmutableModification

[ej3e-64]: https://books.google.com/books?id=BIpDDwAAQBAJ

[javadoc]: https://google.github.io/guava/releases/snapshot-jre/api/docs/com/google/common/collect/ImmutableCollection.html

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MutableConstantField")` to the enclosing element.
