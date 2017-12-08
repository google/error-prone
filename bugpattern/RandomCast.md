---
title: RandomCast
summary: Casting a random number in the range [0.0, 1.0) to an integer or long always results in 0.
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
`Math.random()`, `Random#nextFloat`, and `Random#nextDouble` return results in
the range `[0.0, 1.0)`. Therefore, casting the result to `(int)` or `(long)`
*always* results in the value of `0`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RandomCast")` to the enclosing element.
