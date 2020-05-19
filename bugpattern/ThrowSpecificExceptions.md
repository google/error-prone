---
title: ThrowSpecificExceptions
summary: Consider throwing more specific exceptions rather than (e.g.) RuntimeException. Throwing generic exceptions forces any users of the API that wish to handle the failure mode to catch very non-specific exceptions that convey little information.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ThrowSpecificExceptions")` to the enclosing element.

