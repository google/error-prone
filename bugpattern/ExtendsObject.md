---
title: ExtendsObject
summary: '`T extends Object` is redundant (unless you are using the Checker Framework).'
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`T extends Object` is redundant; both `<T>` and `<T extends Object>` compile to
identical class files.— unless you are using the Checker Framework).

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ExtendsObject")` to the enclosing element.
