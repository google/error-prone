---
title: JodaDurationConstructor
summary: Use of new Duration(long) is not allowed. Please use Duration.millis(long) instead.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Joda-Time's 'new Duration(long)' constructor is ambiguous with respect to time units and is frequently a source of bugs. Please use Duration.millis(long) instead. If your Duration is better expressed in terms of other units, use standardSeconds(long), standardMinutes(long), standardHours(long), or standardDays(long) instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JodaDurationConstructor")` to the enclosing element.
