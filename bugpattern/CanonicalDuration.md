---
title: CanonicalDuration
summary: Duration can be expressed more clearly with different units
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
Prefer to express durations using the largest possible unit, e.g. `Duration.ofDays(1)` instead of `Duration.ofSeconds(86400)`.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("CanonicalDuration")` annotation to the enclosing element.
