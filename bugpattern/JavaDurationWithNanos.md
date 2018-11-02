---
title: JavaDurationWithNanos
summary: Use of java.time.Duration.withNanos(int) is not allowed.
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
Duration's withNanos(int) method is often a source of bugs because it returns a copy of the current Duration instance, but _only_ the nano field is mutated (the seconds field is copied directly). Use Duration.ofSeconds(duration.getSeconds(), nanos) instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JavaDurationWithNanos")` to the enclosing element.
