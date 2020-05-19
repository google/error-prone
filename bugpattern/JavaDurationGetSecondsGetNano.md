---
title: JavaDurationGetSecondsGetNano
summary: duration.getNano() only accesses the underlying nanosecond adjustment from the whole second.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
If you call duration.getNano(), you must also call duration.getSeconds() in 'nearby' code. If you are trying to convert this duration to nanoseconds, you probably meant to use duration.toNanos() instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JavaDurationGetSecondsGetNano")` to the enclosing element.

