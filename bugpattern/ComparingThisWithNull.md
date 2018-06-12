---
title: ComparingThisWithNull
summary: this == null is always false, this != null is always true
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
The boolean expression this != null always returns true and similarly this == null always returns false.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ComparingThisWithNull")` to the enclosing element.
