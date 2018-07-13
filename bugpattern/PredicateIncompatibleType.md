---
title: PredicateIncompatibleType
summary: Using ::equals or ::isInstance as an incompatible Predicate; the predicate will always return false
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Using `Foo::equals` as a `Predicate` for any type that is not compatible with
`Foo` is almost certainly a bug, since the predicate will always return false.

For example, consider:

```java
Predicate<Integer> p = "hello"::equals;
```

See also [EqualsIncompatibleType](EqualsIncompatibleType.md).

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PredicateIncompatibleType")` to the enclosing element.
