---
title: PeriodFrom
summary: Period.from(Period) returns itself; from(Duration) throws a runtime exception.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Period.from(TemporalAmount) will always throw a DateTimeException when passed a Duration and return itself when passed a Period.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PeriodFrom")` to the enclosing element.
