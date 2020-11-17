---
title: UnnecessaryMethodReference
summary: This method reference is unnecessary, and can be replaced with the variable
  itself.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Using a method reference to refer to the abstract method of the target type is
unnecessary. For example,

```java
Stream<Integer> filter(Stream<Integer> xs, Predicate<Integer> predicate) {
  return xs.filter(predicate::test);
}
```

```java
Stream<Integer> filter(Stream<Integer> xs, Predicate<Integer> predicate) {
  return xs.filter(predicate);
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryMethodReference")` to the enclosing element.
