---
title: LabelledBreakTarget
summary: Labels should only be used on loops.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Labels should only be used on loops. For other statements, consider refactoring
to express the control flow without labelled breaks.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("LabelledBreakTarget")` to the enclosing element.
