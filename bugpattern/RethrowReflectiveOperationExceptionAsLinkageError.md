---
title: RethrowReflectiveOperationExceptionAsLinkageError
summary: Prefer LinkageError for rethrowing ReflectiveOperationException as unchecked
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Consider using `LinkageError` instead of `AssertionError` when rethrowing
reflective exceptions as unchecked exceptions, since it conveys more information
when reflection fails due to an incompatible change in the classpath.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RethrowReflectiveOperationExceptionAsLinkageError")` to the enclosing element.
