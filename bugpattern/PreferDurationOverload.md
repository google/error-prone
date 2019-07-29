---
title: PreferDurationOverload
summary: Prefer using java.time-based APIs when available. Note that this checker does not and cannot guarantee that the overloads have equivalent semantics, but that is generally the case with overloaded methods.
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
APIs that accept a java.time.Duration/Instant should be preferred, when available. JodaTime is now considered a legacy library for Java 8+ users. Representing date/time concepts as numeric primitives is strongly discouraged (e.g., long timeout). APIs that require a <long, TimeUnit> pair suffer from a number of problems: 1) they may require plumbing 2 parameters through various layers of your application; 2) overflows are possible when doing any duration math; 3) they lack semantic meaning; 4) decomposing a duration into a <long, TimeUnit> is dangerous because of unit mismatch and/or excessive truncation.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PreferDurationOverload")` to the enclosing element.
