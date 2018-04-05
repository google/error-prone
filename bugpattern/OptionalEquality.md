---
title: OptionalEquality
summary: Comparison using reference equality instead of value equality
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Optionals should be compared for value equality using `.equals()`, and not for
reference equality using `==` and `!=`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("OptionalEquality")` to the enclosing element.
