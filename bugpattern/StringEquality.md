---
title: StringEquality
summary: String comparison using reference equality instead of value equality
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Strings are compared for reference equality/inequality using == or !=instead of
for value equality using .equals()

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StringEquality")` to the enclosing element.
