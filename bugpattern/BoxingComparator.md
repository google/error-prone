---
title: BoxingComparator
summary: Comparator.comparing() unnecessarily boxes numerical primitives; please use
  the primitive-specific method instead (e.g., comparingInt()).
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("BoxingComparator")` to the enclosing element.
