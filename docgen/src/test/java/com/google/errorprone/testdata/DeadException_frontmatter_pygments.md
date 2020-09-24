---
title: DeadException
summary: Exception created but not thrown
layout: bugpattern
tags: LikelyError
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: ThrowableInstanceNeverThrown_

## The problem
The exception is created with new, but is not thrown, and the reference is lost.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DeadException")` to the enclosing element.
