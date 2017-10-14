---
title: ThrowIfUncheckedKnownChecked
summary: throwIfUnchecked(knownCheckedException) is a no-op.
layout: bugpattern
tags: ''
severity: ERROR
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
`throwIfUnchecked(knownCheckedException)` is a no-op (aside from performing a null check). `propagateIfPossible(knownCheckedException)` is a complete no-op.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ThrowIfUncheckedKnownChecked")` annotation to the enclosing element.
