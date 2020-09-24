---
title: SuppressWarningsDeprecated
summary: Suppressing "deprecated" is probably a typo for "deprecation"
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
To suppress warnings to deprecated methods, you should add the annotation
`@SuppressWarnings("deprecation")` and not `@SuppressWarnings("deprecated")`

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SuppressWarningsDeprecated")` to the enclosing element.
