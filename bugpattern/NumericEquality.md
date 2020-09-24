---
title: NumericEquality
summary: Numeric comparison using reference equality instead of value equality
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Numbers are compared for reference equality/inequality using == or != instead of
for value equality using .equals()

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NumericEquality")` to the enclosing element.
