---
title: JavaDurationWithSeconds
summary: Use of java.time.Duration.withSeconds(long) is not allowed.
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
Duration's withSeconds(long) method is often a source of bugs because it returns a copy of the current Duration instance, but _only_ the seconds field is mutated (the nanos field is copied directly). Use Duration.ofSeconds(seconds, duration.getNano()) instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JavaDurationWithSeconds")` to the enclosing element.
