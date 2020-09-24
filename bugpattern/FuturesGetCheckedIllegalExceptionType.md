---
title: FuturesGetCheckedIllegalExceptionType
summary: Futures.getChecked requires a checked exception type with a standard constructor.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The passed exception type must not be a RuntimeException, and it must expose a
public constructor whose only parameters are of type String or Throwable.
getChecked will reject any other type with an IllegalArgumentException.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FuturesGetCheckedIllegalExceptionType")` to the enclosing element.
