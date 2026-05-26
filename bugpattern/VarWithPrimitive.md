---
title: VarWithPrimitive
summary: Avoid using `var` with primitive types. Explicit primitive type names are
  short and clear, and `var` provides no benefit in readability while potentially
  hiding the type.
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("VarWithPrimitive")` to the enclosing element.
