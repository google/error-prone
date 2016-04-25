---
title: JUnit3FloatingPointComparisonWithoutDelta
summary: Floating-point comparison without error tolerance
layout: bugpattern
category: JUNIT
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Use assertEquals(expected, actual, delta) to compare floating-point numbers. This call to assertEquals() will either fail or not compile in JUnit 4.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("JUnit3FloatingPointComparisonWithoutDelta")` annotation to the enclosing element.
