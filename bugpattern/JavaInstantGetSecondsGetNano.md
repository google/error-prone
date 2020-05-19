---
title: JavaInstantGetSecondsGetNano
summary: instant.getNano() only accesses the underlying nanosecond adjustment from the whole second.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
If you call instant.getNano(), you must also call instant.getEpochSecond() in 'nearby' code. If you are trying to convert this instant to nanoseconds, you probably meant to use Instants.toEpochNanos(instant) instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JavaInstantGetSecondsGetNano")` to the enclosing element.

