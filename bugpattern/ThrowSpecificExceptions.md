---
title: ThrowSpecificExceptions
summary: 'Base exception classes should be treated as abstract. If the exception is
  intended to be caught, throw a domain-specific exception. Otherwise, prefer a more
  specific exception for clarity. Common alternatives include: AssertionError, IllegalArgumentException,
  IllegalStateException, and (Guava''s) VerifyException.'
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
1. Defensive coding: Using a generic exception would force a caller that wishes to catch it to potentially catch unrelated exceptions as well.

2. Clarity: Base exception classes offer no information on the nature of the failure.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ThrowSpecificExceptions")` to the enclosing element.
