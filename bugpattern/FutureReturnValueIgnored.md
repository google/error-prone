---
title: FutureReturnValueIgnored
summary: Return value of methods returning Future must be checked. Ignoring returned Futures suppresses exceptions thrown from the code that completes the Future.
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Methods that return `java.util.concurrent.Future` and its subclasses generally
indicate errors by returning a future that eventually fails.

If you don’t check the return value of these methods, you will never find out if
they threw an exception.

Nested futures can also result in missed cancellation signals or suppressed
exceptions - see
[Avoiding Nested Futures](https://github.com/google/guava/wiki/ListenableFutureExplained#avoid-nested-futures)
for details.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FutureReturnValueIgnored")` to the enclosing element.
