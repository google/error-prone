---
title: NullNeedsCastForVarargs
summary: This call passes a null *array*, so it always produces NullPointerException.
  To pass a null *element*, cast to the element type.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Note: The Truth methods covered by this check still accept a literal null in
Truth 1.4.5, but we expect to change that in a forthcoming version.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NullNeedsCastForVarargs")` to the enclosing element.
