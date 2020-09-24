---
title: CannotMockFinalClass
summary: Mockito cannot mock final classes
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Mockito cannot mock final classes. See
https://github.com/mockito/mockito/wiki/FAQ for details.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("CannotMockFinalClass")` to the enclosing element.
