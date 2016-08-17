---
title: IdentityBinaryExpression
summary: Writing "a && a", "a || a", "a & a", or "a | a" is equivalent to "a".
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Writing `a && a`, `a || a`, `a & a`, or `a | a` is equivalent to `a`.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("IdentityBinaryExpression")` annotation to the enclosing element.
