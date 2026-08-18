---
title: HashCodeInObjectsHash
summary: Calling hashCode() or Objects.hash() inside Objects.hash() is redundant;
  use Objects.hashCode() for single-argument calls.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`Objects.hash(...)` computes a combined hash code by accepting a sequence of
values and hashing each one. Passing `.hashCode()` or a nested `Objects.hash()`
/ `Objects.hashCode()` call as an argument is redundant and often indicates
confusion about how `Objects.hash` works.

For example, instead of:

```java
@Override
public int hashCode() {
  return Objects.hash(foo, bar.hashCode());
}
```

or:

```java
@Override
public int hashCode() {
  return Objects.hash(foo, Objects.hash(bar));
}
```

Prefer passing the values directly:

```java
@Override
public int hashCode() {
  return Objects.hash(foo, bar);
}
```

Similarly, when calling `Objects.hash` with only a single argument, prefer
`Objects.hashCode(...)` instead:

```java
// Prefer:
return Objects.hashCode(foo);

// Instead of:
return Objects.hash(foo);
```

`Objects.hash(...)` handles `null` references safely (treating `null` as `0`),
so removing the `.hashCode()` call also avoids potential
`NullPointerException`s.

### Exceptions

*   **`super.hashCode()`**: Calling `super.hashCode()` is permitted when
    incorporating a superclass's hash code computation.
*   **`Arrays.hashCode(...)` / `Arrays.deepHashCode(...)`**: Arrays do not
    override `Object.hashCode()`, so calling `Arrays.hashCode()` or
    `Arrays.deepHashCode()` is the recommended way to hash array elements within
    `Objects.hash(...)`.
*   **`System.identityHashCode(...)`**: Identity hashing intentionally differs
    from `Object.hashCode()`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("HashCodeInObjectsHash")` to the enclosing element.
