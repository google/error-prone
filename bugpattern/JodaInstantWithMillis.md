---
title: JodaInstantWithMillis
summary: Use of instant.withMillis(long) is not allowed. Please use new Instant(long) instead.
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
Joda-Time's 'instant.withMillis(long)' method is often a source of bugs because it doesn't mutate the current instance but rather returns a new immutable Instant instance. Please use new Instant(long) instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JodaInstantWithMillis")` to the enclosing element.
