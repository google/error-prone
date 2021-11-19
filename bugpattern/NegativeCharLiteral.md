---
title: NegativeCharLiteral
summary: Casting a negative signed literal to an (unsigned) char might be misleading.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NegativeCharLiteral")` to the enclosing element.
