---
title: RestrictedApiChecker
summary: Check for non-allowlisted callers to RestrictedApiChecker.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Calls to APIs marked @RestrictedApi are prohibited without a corresponding
allowlist annotation.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RestrictedApiChecker")` to the enclosing element.
