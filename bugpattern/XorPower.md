---
title: XorPower
summary: The `^` operator is binary XOR, not a power operator.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The `^` binary XOR operator is sometimes mistaken for a power operator, but e.g.
`2 ^ 2` evaluates to `0`, not `4`.

Consider expressing powers of `2` using a bit shift instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("XorPower")` to the enclosing element.
