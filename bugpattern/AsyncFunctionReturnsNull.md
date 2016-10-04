---
title: AsyncFunctionReturnsNull
summary: AsyncFunction should not return a null Future, only a Future whose result is null.
layout: bugpattern
category: GUAVA
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Methods like Futures.transformAsync and Futures.catchingAsync will throw a NullPointerException if the provided AsyncFunction returns a null Future. To produce a Future with an output of null, instead return immediateFuture(null).

## Suppression
Suppress false positives by adding an `@SuppressWarnings("AsyncFunctionReturnsNull")` annotation to the enclosing element.
