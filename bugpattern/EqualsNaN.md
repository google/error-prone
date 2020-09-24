---
title: EqualsNaN
summary: == NaN always returns false; use the isNaN methods instead
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
As per JLS 15.21.1, == NaN comparisons always return false, even NaN == NaN.
Instead, use the isNaN methods to check for NaN.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("EqualsNaN")` to the enclosing element.
