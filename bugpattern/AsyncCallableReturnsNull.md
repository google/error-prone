---
title: AsyncCallableReturnsNull
summary: AsyncCallable should not return a null Future, only a Future whose result is null.
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Methods like Futures.whenAllComplete(...).callAsync(...) will throw a
NullPointerException if the provided AsyncCallable returns a null Future. To
produce a Future with an output of null, instead return immediateFuture(null).

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AsyncCallableReturnsNull")` to the enclosing element.
