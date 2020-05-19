---
title: JodaDurationWithMillis
summary: Use of duration.withMillis(long) is not allowed. Please use Duration.millis(long) instead.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Joda-Time's 'duration.withMillis(long)' method is often a source of bugs because it doesn't mutate the current instance but rather returns a new immutable Duration instance.Please use Duration.millis(long) instead. If your Duration is better expressed in terms of other units, use standardSeconds(long), standardMinutes(long), standardHours(long), or standardDays(long) instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JodaDurationWithMillis")` to the enclosing element.

