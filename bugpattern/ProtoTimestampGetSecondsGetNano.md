---
title: ProtoTimestampGetSecondsGetNano
summary: getNanos() only accesses the underlying nanosecond-adjustment of the instant.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
If you call timestamp.getNanos(), you must also call timestamp.getSeconds() in 'nearby' code. If you are trying to convert this timestamp to nanoseconds, you probably meant to use Timestamps.toNanos(timestamp) instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ProtoTimestampGetSecondsGetNano")` to the enclosing element.
