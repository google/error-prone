---
title: AlreadyChecked
summary: This condition has already been checked.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Checking a provably-identical condition twice can be a sign of a logic error.

For example:

```java
public Optional<T> first(Optional<T> a, Optional<T> b) {
  if (a.isPresent()) {
    return a;
  } else if (a.isPresent()) { // Oops--should be checking `b`.
    return b;
  } else {
    return Optional.empty();
  }
}
```

It can also be a sign of redundancy, which can just be removed.

```java
public void act() {
  if (enabled) {
    frobnicate();
  } else if (!enabled) { // !enabled is guaranteed to be true here, so the check can be removed
    doSomethingElse();
  }
}

```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AlreadyChecked")` to the enclosing element.
