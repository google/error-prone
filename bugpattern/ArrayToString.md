---
title: ArrayToString
summary: Calling toString on an array does not provide useful information
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The `toString` method on an array will print its identity, such as
`[I@4488aabb`. This is almost never needed. Use `Arrays.toString` to print a
human-readable summary.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ArrayToString")` to the enclosing element.
