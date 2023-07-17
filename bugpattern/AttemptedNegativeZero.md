---
title: AttemptedNegativeZero
summary: -0 is the same as 0. For the floating-point negative zero, use -0.0.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Because `0` is an integer constant, `-0` is an integer, too. Integers have no
concept of "negative zero," so it is the same as plain `0`.

The value is then widened to a floating-point number. And while floating-point
numbers have a concept of "negative zero," the integral `0` is widened to the
floating-point "positive" zero.

To write a negative zero, you have to write a constant that is a floating-point
number. One simple way to do that is to write `-0.0`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AttemptedNegativeZero")` to the enclosing element.
