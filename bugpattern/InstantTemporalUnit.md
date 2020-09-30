---
title: InstantTemporalUnit
summary: Instant APIs only work for NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS,
  HALF_DAYS and DAYS.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InstantTemporalUnit")` to the enclosing element.
