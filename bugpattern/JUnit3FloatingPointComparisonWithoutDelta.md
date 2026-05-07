---
title: JUnit3FloatingPointComparisonWithoutDelta
summary: Floating-point comparison without error tolerance
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
JUnit 4's floating-point overloads of `assertEquals(expected, actual)` always
throw an exception, and some floating-point calls to JUnit 4's `assertEquals` do
not even compile.

To continue comparing floating-point numbers using `Double.equals` semantics,
you may be able to cast one argument to `Object` or use Truth's
`assertThat(actual).isEqualTo(expected)` /
`assertWithMessage(message).that(actual).isEqualTo(expected)`.

Alternatively, you can switch to tolerance-based equality testing, which changes
your code's behavior for negative zero (in JUnit and Truth) and for infinities
and NaN (in Truth). If you want that, use JUnit's `assertEquals(expected,
actual, delta)` or Truth's `isWithin(...).of(...)`, possibly with a tolerance of
zero.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JUnit3FloatingPointComparisonWithoutDelta")` to the enclosing element.
