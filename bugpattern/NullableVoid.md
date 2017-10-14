---
title: NullableVoid
summary: void-returning methods should not be annotated with @Nullable, since they cannot return null
layout: bugpattern
tags: Style
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
void-returning methods cannot return null.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("NullableVoid")` annotation to the enclosing element.
