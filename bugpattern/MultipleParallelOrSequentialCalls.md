---
title: MultipleParallelOrSequentialCalls
summary: Multiple calls to either parallel or sequential are unnecessary and cause confusion.
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
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MultipleParallelOrSequentialCalls")` to the enclosing element.
