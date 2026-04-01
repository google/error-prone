---
title: MemorySegmentReferenceEquality
summary: Do not compare MemorySegments using reference equality.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`MemorySegment` is a value-based class. Comparing `MemorySegment` instances
using `==` or `!=` is bug-prone because `MemorySegment` implementations may not
be unique for the same underlying memory. Use `Objects.equals()` (or `.equals()`
if the receiver is known to be non-null) instead.

For example:

```java
MemorySegment seg = ...;
if (seg == MemorySegment.NULL) { // reference equality
  ...
}
```

should be:

```java
MemorySegment seg = ...;
if (Objects.equals(seg, MemorySegment.NULL)) { // value equality
  ...
}
```

Reference equality between any two `MemorySegment` instances (e.g., `a == b`) is
flagged by this check, and `Objects.equals(a, b)` is the preferred alternative.

See also https://bugs.openjdk.org/browse/JDK-8381012

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MemorySegmentReferenceEquality")` to the enclosing element.
