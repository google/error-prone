---
title: NullOptional
summary: Passing a literal null to an Optional parameter is almost certainly a mistake. Did you mean to provide an empty Optional?
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Passing a literal `null` to an `Optional` accepting parameter is likely a bug.
`Optional` is already designed to encode missing values through a non-`null`
instance.

```java
Optional<Integer> double(Optional<Integer> i) {
  return i.map(i -> i * 2);
}

Optional<Integer> doubled = double(null);
```

```java
Optional<Integer> doubled = double(Optional.empty());
```

This is a scenario that can easily happen when refactoring code from accepting
`@Nullable` parameters to accept `Optional`s. Note that the check will not match
if the parameter is explicitly annotated `@Nullable`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NullOptional")` to the enclosing element.
