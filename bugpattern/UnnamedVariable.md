---
title: UnnamedVariable
summary: Use the unnamed variable syntax (`_`) for unused variables and lambda parameters.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Prefer using an unnamed variable (`_`) to denote variables and patterns that are
intentionally unused.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnamedVariable")` to the enclosing element.
