---
title: UnnecessaryBoxedAssignment
summary: This expression can be implicitly boxed.
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
It is unnecessary for this assignment or return expression to be boxed explicitly.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryBoxedAssignment")` to the enclosing element.

