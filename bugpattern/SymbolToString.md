---
title: SymbolToString
summary: Symbol#toString shouldn't be used for comparison as it is expensive and fragile.
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SymbolToString")` to the enclosing element.
