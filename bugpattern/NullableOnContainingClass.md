---
title: NullableOnContainingClass
summary: Type-use nullability annotations should annotate the inner class, not the
  outer class (e.g., write `A.@Nullable B` instead of `@Nullable A.B`).
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NullableOnContainingClass")` to the enclosing element.
