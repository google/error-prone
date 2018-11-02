---
title: JodaWithDurationAddedLong
summary: Use of JodaTime's type.withDurationAdded(long, int) (where <type> = {Duration,Instant,DateTime}). Please use type.withDurationAdded(Duration.millis(long), int) instead.
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
JodaTime's type.withDurationAdded(long, int) is often a source of bugs because the units of the parameters are ambiguous. Please use type.withDurationAdded(Duration.millis(long), int) instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JodaWithDurationAddedLong")` to the enclosing element.
