---
title: PeriodGetTemporalUnit
summary: Period.get() only works with YEARS, MONTHS, or DAYS.
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
`Period.get(TemporalUnit)` only works when passed `ChronoUnit.YEARS`, `ChronoUnit.MONTHS`, or `ChronoUnit.DAYS`. All other values are guaranteed to throw an `UnsupportedTemporalTypeException`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PeriodGetTemporalUnit")` to the enclosing element.
