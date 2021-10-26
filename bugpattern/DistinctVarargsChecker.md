---
title: DistinctVarargsChecker
summary: Method expects distinct arguments at some/all positions
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Various methods which take variable-length arguments throw the runtime
exceptions like `IllegalArgumentException` when the arguments are not distinct.

This checker warns on using the non-distinct parameters in various varargs
method when the usage is redundant or will either result in the runtime
exception.

Bad:

```java
ImmutableSet.of(first, second, second, third);
```

Good:

```java
ImmutableSet.of(first, second, third);
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DistinctVarargsChecker")` to the enclosing element.
