---
title: ShortCircuitBoolean
summary: Prefer the short-circuiting boolean operators && and || to & and |.
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The boolean operators `&&` and `||` should almost always be used instead of `&`
and `|`.

If the right hand side is an expression that has side effects or is expensive to
compute, `&&` and `||` will short-circuit but `&` and `|` will not, which may be
surprising or cause slowness.

If evaluating both operands is necessary for side effects, consider refactoring
to make that explicit. For example, prefer this:

```java
int rhs = hasSideEffects();
if (lhs && rhs) {
  // ...
}
```

to this:

```java
if (lhs & hasSideEffects()) {
  // ...
}
```

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ShortCircuitBoolean")` annotation to the enclosing element.
