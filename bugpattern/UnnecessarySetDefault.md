---
title: UnnecessarySetDefault
summary: Unnecessary call to NullPointerTester#setDefault
layout: bugpattern
tags: ''
severity: SUGGESTION
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
NullPointerTester comes with built-in support for some well known types like
`Optional` and `ImmutableList` via guava's
[`ArbitraryInstances`](http://static.javadoc.io/com.google.guava/guava-testlib/23.0/com/google/common/testing/ArbitraryInstances.html)
class. Explicitly calling `setDefault` for these types is unnecessary.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessarySetDefault")` to the enclosing element.
