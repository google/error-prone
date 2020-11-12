---
title: LocalDateTemporalAmount
summary: LocalDate.plus() and minus() does not work with Durations. LocalDate represents
  civil time (years/months/days), so java.time.Period is the appropriate thing to
  add or subtract instead.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("LocalDateTemporalAmount")` to the enclosing element.
