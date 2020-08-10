---
title: JodaToSelf
summary: Use of Joda-Time's DateTime.toDateTime(), Duration.toDuration(), Instant.toInstant(), Interval.toInterval(), and Period.toPeriod() are not allowed.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Joda-Time's DateTime.toDateTime(), Duration.toDuration(), Instant.toInstant(), Interval.toInterval(), and Period.toPeriod() are always unnecessary, since they simply 'return this'. There is no reason to ever call them.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JodaToSelf")` to the enclosing element.
