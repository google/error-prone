---
title: RequiredModifiers
summary: This annotation is missing required modifiers as specified by its @RequiredModifiers annotation
layout: bugpattern
tags: LikelyError
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
This annotation is itself annotated with @RequiredModifiers and can only be used when the specified modifiers are present. You are attempting touse it on an  element that is missing one or more required modifiers.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RequiredModifiers")` to the enclosing element.
