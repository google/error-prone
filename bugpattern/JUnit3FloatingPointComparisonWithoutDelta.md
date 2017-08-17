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
Use assertEquals(expected, actual, delta) to compare floating-point numbers. This call to assertEquals() will either fail or not compile in JUnit 4. Use assertEquals(expected, actual, 0.0) if the delta must be 0.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("JUnit3FloatingPointComparisonWithoutDelta")` annotation to the enclosing element.
