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
`a && a`, `a || a`, `a & a`, or `a | a`
:   equivalent to `a`

`a <= a`, `a >= a`, or `a == a`
:   always `true`

`a < a`, `a > a`, `a != a`, or `a ^ a`
:   always `false`

`a / a`
:   always `1`

`a % a` or `a - a`
:   always `0`

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("IdentityBinaryExpression")` to the enclosing element.
