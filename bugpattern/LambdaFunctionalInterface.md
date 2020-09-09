---
title: LambdaFunctionalInterface
summary: Use Java's utility functional interfaces instead of Function<A, B> for primitive types.
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Prefer specialized functional interface types for primitives, for example
`IntToLongFunction` instead of `Function<Integer, Long>`, to avoid boxing
overhead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("LambdaFunctionalInterface")` to the enclosing element.
