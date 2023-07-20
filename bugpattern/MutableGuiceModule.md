---
title: MutableGuiceModule
summary: Fields in Guice modules should be final
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Guice modules should not be mutable.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MutableGuiceModule")` to the enclosing element.
