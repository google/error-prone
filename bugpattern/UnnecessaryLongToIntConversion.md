---
title: UnnecessaryLongToIntConversion
summary: Converting a long or Long to an int to pass as a long parameter is usually
  not necessary. If this conversion is intentional, consider `Longs.constrainToRange()`
  instead.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryLongToIntConversion")` to the enclosing element.
