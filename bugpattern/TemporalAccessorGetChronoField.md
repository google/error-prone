---
title: TemporalAccessorGetChronoField
summary: TemporalAccessor.get() only works for certain values of ChronoField.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
TemporalAccessor.get(ChronoField) only works for certain values of ChronoField. E.g., DayOfWeek only supports DAY_OF_WEEK. All other values are guaranteed to throw an UnsupportedTemporalTypeException.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TemporalAccessorGetChronoField")` to the enclosing element.
