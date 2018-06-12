---
title: SubstringOfZero
summary: String.substring(0) returns the original String
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
String.substring(int) gives you the substring from the index to the end, inclusive.Calling that method with an index of 0 will return the same String.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SubstringOfZero")` to the enclosing element.
