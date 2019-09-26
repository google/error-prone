---
title: ProtoDurationGetSecondsGetNano
summary: getNanos() only accesses the underlying nanosecond-adjustment of the duration.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
If you call duration.getNanos(), you must also call duration.getSeconds() in 'nearby' code. If you are trying to convert this duration to nanoseconds, you probably meant to use Durations.toNanos(duration) instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ProtoDurationGetSecondsGetNano")` to the enclosing element.
