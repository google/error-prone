---
title: DivZero
summary: Division by integer literal zero
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: divzero_

## The problem
This code will cause a runtime arithmetic exception if it is executed.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DivZero")` to the enclosing element.
