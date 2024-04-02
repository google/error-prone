---
title: RestrictedApi
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
Calls to APIs marked `@RestrictedApi` are prohibited without a corresponding
allowlist annotation.

The intended use-case for `@RestrictedApi` is to restrict calls to annotated
methods so that each usage of those APIs must be reviewed separately. For
example, an API might lead to security bugs unless the programmer uses it
correctly.

See the
[javadoc for `@RestrictedApi`](https://errorprone.info/api/latest/com/google/errorprone/annotations/RestrictedApi.html)
for more details.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RestrictedApi")` to the enclosing element.
