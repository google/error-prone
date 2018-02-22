---
title: IdentityBinaryExpression
summary: A binary expression where both operands are the same is usually incorrect.
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: SelfEquality_

## The problem
Using the same expressions as both arguments to the following binary expressions
is usually a mistake:

*   `a && a`, `a || a`, `a & a`, or `a | a` is equivalent to `a`
*   `a <= a`, `a >= a`, or `a == a` is always `true`
*   `a < a`, `a > a`, `a != a`, or `a ^ a` is always `false`
*   `a / a` is always `1`
*   `a % a` or `a - a` is always `0`

If the expression has side-effects, consider refactoring one of the expressions
with side effects into a local. For example, prefer this:

```.java {.good}
// check twice, just to be sure
boolean isTrue = foo.isTrue();
if (isTrue && foo.isTrue()) {
  // ...
}
```

to this:

```.java {.bad}
if (foo.isTrue() && foo.isTrue()) {
  // ...
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("IdentityBinaryExpression")` to the enclosing element.
