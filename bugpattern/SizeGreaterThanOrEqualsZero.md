---
title: SizeGreaterThanOrEqualsZero
summary: Comparison of a size >= 0 is always true, did you intend to check for non-emptiness?
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
A standard means of checking non-emptiness of an array or collection is to test
if the size of that collection is greater than 0. However, one may accidentally
check if the size is greater than or equal to 0, which is always true.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SizeGreaterThanOrEqualsZero")` to the enclosing element.
