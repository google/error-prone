---
title: PredicateIncompatibleType
summary: Using ::equals as an incompatible Predicate; the predicate will always return false
layout: bugpattern
tags: ''
severity: ERROR
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
Suppress false positives by adding an `@SuppressWarnings("PredicateIncompatibleType")` annotation to the enclosing element.
