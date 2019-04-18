---
title: UnnecessaryBoxedVariable
summary: It is unnecessary for this variable to be boxed. Use the primitive instead.
layout: bugpattern
tags: ''
severity: SUGGESTION
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
This variable is of boxed type, but is always unboxed before use. Make it primitive instead

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryBoxedVariable")` to the enclosing element.
