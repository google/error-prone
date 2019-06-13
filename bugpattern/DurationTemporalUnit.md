---
title: DurationTemporalUnit
summary: Duration APIs only work for DAYS or exact durations.
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
Duration APIs only work for TemporalUnits with exact durations or ChronoUnit.DAYS. E.g., Duration.of(1, ChronoUnit.YEARS) is guaranteed to throw a DateTimeException.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DurationTemporalUnit")` to the enclosing element.
