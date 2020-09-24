---
title: ComparableType
summary: Implementing 'Comparable<T>' where T is not the same as the implementing class is incorrect, since it violates the symmetry contract of compareTo.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The type argument of `Comparable` should always be the type of the current
class.

For example, do this:

```java
class Foo implements Comparable<Foo> {
  public int compareTo(Foo other) { ... }
}
```

not this:

```java
class Foo implements Comparable<Bar> {
  public int compareTo(Foo other) { ... }
}
```

Implementing `Comparable` for a different type breaks the API contract, which
requires `x.compareTo(y) == -y.compareTo(x)` for all `x` and `y`. If `x` and `y`
are different types, this behaviour can't be guaranteed.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ComparableType")` to the enclosing element.
