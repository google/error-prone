---
title: MathRoundIntLong
summary: Math.round(Integer) results in truncation
layout: bugpattern
tags: ''
severity: ERROR
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Math.round() called with an integer or long type results in truncationbecause Math.round only accepts floats or doubles and some integers and longs can'tbe represented with float.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MathRoundIntLong")` to the enclosing element.
