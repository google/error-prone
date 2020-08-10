---
title: DurationGetTemporalUnit
summary: Duration.get() only works with SECONDS or NANOS.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`Duration.get(TemporalUnit)` only works when passed `ChronoUnit.SECONDS` or `ChronoUnit.NANOS`. All other values are guaranteed to throw a `UnsupportedTemporalTypeException`. In general, you should avoid `duration.get(ChronoUnit)`. Instead, please use `duration.toNanos()`, `Durations.toMicros(duration)`, `duration.toMillis()`, `duration.getSeconds()`, `duration.toMinutes()`, `duration.toHours()`, or `duration.toDays()`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DurationGetTemporalUnit")` to the enclosing element.
