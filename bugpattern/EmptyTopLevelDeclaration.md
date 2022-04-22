---
title: EmptyTopLevelDeclaration
summary: Empty top-level type declarations should be omitted
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
A semi-colon at the top level of a Java file is treated as an empty type
declaration in the grammar, but it's confusing and unnecessary.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("EmptyTopLevelDeclaration")` to the enclosing element.
