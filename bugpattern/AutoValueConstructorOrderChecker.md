---
title: AutoValueConstructorOrderChecker
summary: Arguments to AutoValue constructor are in the wrong order
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
AutoValue constructors are synthesized with their parameters in the same order
as the abstract accessor methods. Calls to the constructor need to match this
ordering.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AutoValueConstructorOrderChecker")` to the enclosing element.
