---
title: MutablePublicArray
summary: Non-empty arrays are mutable, so this `public static final` array is not a constant and can be modified by clients of this class.  Prefer an ImmutableList, or provide an accessor method that returns a defensive copy.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Nonzero-length arrays are mutable. Declaring one `public static final` indicates
that the developer expects it to be a constant, which is not the case. Making it
`public` is especially dangerous since clients of this code can modify the
contents of the array.

There are two ways to fix this problem:

1.  Refactor the array to an `ImmutableList`.
2.  Make the array `private` and add a `public` method that returns a copy of
    the `private` array.

See Effective Java 3rd Edition, Item 15, for more details.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MutablePublicArray")` to the enclosing element.

