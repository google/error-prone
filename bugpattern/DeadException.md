---
title: DeadException
summary: Exception created but not thrown
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: ThrowableInstanceNeverThrown_

## The problem
The exception is created with `new`, but is not thrown, and the reference is
lost.

Creating an exception without using it is unlikely to be correct, so we assume
that you wanted to throw the exception.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DeadException")` to the enclosing element.
