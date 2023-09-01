---
title: UnnecessaryAsync
summary: Variables which are initialized and do not escape the current scope do not
  need to worry about concurrency. Using the non-concurrent type will reduce overhead
  and verbosity.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryAsync")` to the enclosing element.
