---
title: NullablePrimitive
summary: '@Nullable should not be used for primitive types since they cannot be null'
layout: bugpattern
tags: Style
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Primitives can never be null.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NullablePrimitive")` to the enclosing element.
