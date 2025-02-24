---
title: RedundantControlFlow
summary: This continue statement is redundant and can be removed. It may be misleading.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Unnecessary control flow statements may be misleading in some contexts.

For instance, this method to find groups of prime order has a bug:

```java
static ImmutableList<Group> filterGroupsOfPrimeOrder(Iterable<Group> groups) {
  ImmutableList.Builder<Group> filtered = ImmutableList.builder();
  for (Group group : groups) {
    for (int i = 2; i < group.order(); i++) {
      if (group.order() % i == 0) {
        continue;
      }
    }
    filtered.add(group);
  }
  return filtered.build();
}
```

The `continue` statement only breaks out of the innermost loop, so the input is
returned unchanged.

The most readable alternative is probably to avoid a nested loop entirely:

```java
static ImmutableList<Group> filterGroupsOfPrimeOrder(Iterable<Group> groups) {
  ImmutableList.Builder<Group> filtered = ImmutableList.builder();
  for (Group group : groups) {
    if (!isPrime(group.order())) {
      continue;
    }
    filtered.add(group);
  }
  return filtered.build();
}
```

A labelled break statement would also be correct, but is quite uncommon:

```java
static ImmutableList<Group> filterGroupsOfPrimeOrder(Iterable<Group> groups) {
  ImmutableList.Builder<Group> filtered = ImmutableList.builder();
  outer:
  for (Group group : groups) {
    for (int i = 2; i < group.order(); i++) {
      if (group.order() % i == 0) {
        continue outer;
      }
    }
    filtered.add(group);
  }
  return filtered.build();
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RedundantControlFlow")` to the enclosing element.
