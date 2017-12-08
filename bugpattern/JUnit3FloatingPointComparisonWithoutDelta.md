---
title: JUnit3FloatingPointComparisonWithoutDelta
summary: Floating-point comparison without error tolerance
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Use assertEquals(expected, actual, delta) to compare floating-point numbers. This call to assertEquals() will either fail or not compile in JUnit 4. Use assertEquals(expected, actual, 0.0) if the delta must be 0.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JUnit3FloatingPointComparisonWithoutDelta")` to the enclosing element.
