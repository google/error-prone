---
title: ArrayRecordComponent
summary: Record components should not be arrays.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
There are two main problems with having a component of a record be an array.

1.  By default, the generated `equals` and `hashCode` will just call `equals` or
    `hashCode` on the array. Two distinct arrays are never considered equal by
    `equals` even if their contents are the same. The generated `toString` is
    similarly not useful, since it will be something like `[B@723279cf`.

2.  Arrays are mutable, but records should not be mutable. A client of a record
    with an array component can change the contents of the array.

Instead of an array component, consider something like `ImmutableList<String>`,
or, for primitive arrays, something like `ByteString` or `ImmutableIntArray`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ArrayRecordComponent")` to the enclosing element.