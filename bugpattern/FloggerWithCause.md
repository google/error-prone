---
title: FloggerWithCause
summary: Calling withCause(Throwable) with an inline allocated Throwable is discouraged.
  Consider using withStackTrace(StackSize) instead, and specifying a reduced stack
  size (e.g. SMALL, MEDIUM or LARGE) instead of FULL, to improve performance.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FloggerWithCause")` to the enclosing element.
