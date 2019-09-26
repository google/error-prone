---
title: PeriodTimeMath
summary: When adding or subtracting from a Period, Duration is incompatible.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Period.(plus|minus)(TemporalAmount) will always throw a DateTimeException when passed a Duration.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PeriodTimeMath")` to the enclosing element.
