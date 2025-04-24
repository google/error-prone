---
title: PreferInstanceofOverGetKind
summary: Prefer instanceof over getKind() checks where possible, as these work well
  with pattern matching instanceofs
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PreferInstanceofOverGetKind")` to the enclosing element.
