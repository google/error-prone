---
title: EqualsReference
summary: == must be used in equals method to check equality to itself or an infinite
  loop will occur.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
.equals() to the same object will result in infinite recursion

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("EqualsReference")` to the enclosing element.
