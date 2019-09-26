---
title: DurationFrom
summary: Duration.from(Duration) returns itself; from(Period) throws a runtime exception.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Duration.from(TemporalAmount) will always throw a UnsupportedTemporalTypeException when passed a Period and return itself when passed a Duration.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DurationFrom")` to the enclosing element.
